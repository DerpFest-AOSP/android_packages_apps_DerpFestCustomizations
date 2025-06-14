/*
 * SPDX-FileCopyrightText: Project Fluid
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStreamReader

class Misc : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    private val TAG = "DerpFestCustomizations"
    private val KEYBOX_DATA_KEY = "keybox_data_setting"
    private val KEYBOX_DELETE_KEY = "keybox_data_delete"
    private val REQUEST_KEYBOX_FILE = 10003

    private lateinit var mKeyboxDataPreference: Preference
    private lateinit var mKeyboxDeletePreference: Preference

    private val mKeyboxFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val uri = result.data?.data
            if (uri != null) {
                loadKeyboxFile(uri)
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.misc)

        mKeyboxDataPreference = findPreference(KEYBOX_DATA_KEY)!!
        mKeyboxDeletePreference = findPreference(KEYBOX_DELETE_KEY)!!

        mKeyboxDataPreference.setOnPreferenceClickListener {
            openKeyboxFileSelector()
            true
        }

        mKeyboxDeletePreference.setOnPreferenceClickListener {
            deleteKeyboxData()
            true
        }
    }

    private fun openKeyboxFileSelector() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "text/xml"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        mKeyboxFilePickerLauncher.launch(intent)
    }

    private fun deleteKeyboxData() {
        Settings.Secure.putString(
            requireContext().contentResolver,
            Settings.Secure.KEYBOX_DATA,
            null
        )
        Toast.makeText(context, R.string.keybox_data_cleared, Toast.LENGTH_SHORT).show()
    }

    private fun loadKeyboxFile(uri: Uri) {
        Log.d(TAG, "Loading Keybox XML file from URI: ${uri.toString()}")
        
        if (uri.toString().endsWith(".xml") || 
            "text/xml" == requireContext().contentResolver.getType(uri)) {
            try {
                requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val xmlContent = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        xmlContent.append(line).append('\n')
                    }

                    val xml = xmlContent.toString()
                    if (validateKeyboxXml(xml)) {
                        Settings.Secure.putString(
                            requireContext().contentResolver,
                            Settings.Secure.KEYBOX_DATA,
                            xml
                        )
                        Toast.makeText(context, R.string.keybox_data_loaded, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, R.string.keybox_data_invalid, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read keybox XML file", e)
                Toast.makeText(context, R.string.keybox_data_error, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, R.string.keybox_data_invalid, Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateKeyboxXml(xml: String): Boolean {
        var hasPrivKey = false
        var hasEcdsaKey = false
        var hasRsaKey = false
        var ecdsaCertCount = 0
        var rsaCertCount = 0
        var numberOfKeyboxes = -1
        var currentAlg: String? = null

        try {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(xml.reader())

            var eventType = parser.next()
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "NumberOfKeyboxes" -> {
                            parser.next()
                            if (parser.eventType == XmlPullParser.TEXT) {
                                numberOfKeyboxes = parser.text.trim().toIntOrNull() ?: -1
                            }
                        }
                        "Key" -> {
                            currentAlg = parser.getAttributeValue(null, "algorithm")
                            when (currentAlg?.lowercase()) {
                                "ecdsa" -> hasEcdsaKey = true
                                "rsa" -> hasRsaKey = true
                                else -> currentAlg = null
                            }
                        }
                        "PrivateKey" -> hasPrivKey = true
                        "Certificate" -> {
                            when (currentAlg?.lowercase()) {
                                "ecdsa" -> ecdsaCertCount++
                                "rsa" -> rsaCertCount++
                            }
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG && parser.name == "Key") {
                    currentAlg = null
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "XML validation failed", e)
            return false
        }

        return numberOfKeyboxes == 1 &&
               hasPrivKey &&
               hasEcdsaKey && hasRsaKey &&
               ecdsaCertCount == 3 && rsaCertCount == 3
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        return true
    }

    override fun getMetricsCategory(): Int = MetricsEvent.DERPFEST

    companion object {
        const val TAG = "DerpFestCustomizations"
    }
}
