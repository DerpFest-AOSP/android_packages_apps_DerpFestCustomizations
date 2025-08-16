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
import androidx.preference.SwitchPreferenceCompat
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
    private val PIF_DATA_KEY = "pif_data_setting"
    private val PIF_DELETE_KEY = "pif_data_delete"

    private lateinit var mKeyboxDataPreference: Preference
    private lateinit var mKeyboxDeletePreference: Preference
    private lateinit var mPifDataPreference: Preference
    private lateinit var mPifDeletePreference: Preference
    private var mMicCameraPrivacy: SwitchPreferenceCompat? = null
    private var mLocationPrivacy: SwitchPreferenceCompat? = null

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

    private val mPifFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val uri = result.data?.data
            if (uri != null) {
                loadPifFile(uri)
            }
        }
    }

    private fun updateKeyboxSummaries() {
        val keyboxData = Settings.Secure.getString(
            requireContext().contentResolver,
            Settings.Secure.KEYBOX_DATA
        )
        val keyboxTimestamp = Settings.Secure.getString(
            requireContext().contentResolver,
            Settings.Secure.KEYBOX_DATA_TIMESTAMP
        )

        if (keyboxData != null) {
            val keyboxInfo = parseKeyboxInfo(keyboxData, keyboxTimestamp)
            mKeyboxDataPreference.summary = getString(
                R.string.keybox_data_summary_loaded,
                keyboxInfo.type,
                keyboxInfo.certCount,
                keyboxInfo.timestamp
            )
            mKeyboxDeletePreference.isEnabled = true
        } else {
            mKeyboxDataPreference.summary = getString(R.string.keybox_data_summary)
            mKeyboxDeletePreference.isEnabled = false
        }
    }

    private fun updatePifSummaries() {
        val pifData = Settings.Secure.getString(
            requireContext().contentResolver,
            Settings.Secure.PIF_DATA
        )
        val pifTimestamp = Settings.Secure.getString(
            requireContext().contentResolver,
            Settings.Secure.PIF_DATA_TIMESTAMP
        )

        if (pifData != null) {
            val pifInfo = parsePifInfo(pifData, pifTimestamp)
            mPifDataPreference.summary = getString(
                R.string.pif_data_summary_loaded,
                pifInfo.propCount,
                pifInfo.timestamp
            )
            mPifDeletePreference.isEnabled = true
        } else {
            mPifDataPreference.summary = getString(R.string.pif_data_summary)
            mPifDeletePreference.isEnabled = false
        }
    }

    private data class KeyboxInfo(
        val type: String,
        val certCount: Int,
        val timestamp: String
    )

    private data class PifInfo(
        val propCount: Int,
        val timestamp: String
    )

    private fun parseKeyboxInfo(xml: String, timestamp: String? = null): KeyboxInfo {
        var hasEcdsaKey = false
        var hasRsaKey = false
        var certCount = 0

        try {
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(xml.reader())

            var eventType = parser.next()
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "Key" -> {
                            val alg = parser.getAttributeValue(null, "algorithm")?.lowercase()
                            when (alg) {
                                "ecdsa" -> hasEcdsaKey = true
                                "rsa" -> hasRsaKey = true
                            }
                        }
                        "Certificate" -> certCount++
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse keybox info", e)
        }

        val type = when {
            hasEcdsaKey && hasRsaKey -> "RSA + ECDSA"
            hasEcdsaKey -> "ECDSA"
            hasRsaKey -> "RSA"
            else -> "Unknown"
        }

        val displayTimestamp = timestamp ?: java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())

        return KeyboxInfo(
            type = type,
            certCount = certCount,
            timestamp = displayTimestamp
        )
    }

    private fun parsePifInfo(json: String, timestamp: String? = null): PifInfo {
        var propCount = 0

        try {
            val jsonObject = org.json.JSONObject(json)
            propCount = jsonObject.length()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse PIF info", e)
        }

        val displayTimestamp = timestamp ?: java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())

        return PifInfo(
            propCount = propCount,
            timestamp = displayTimestamp
        )
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

        updateKeyboxSummaries()

        mPifDataPreference = findPreference(PIF_DATA_KEY)!!
        mPifDeletePreference = findPreference(PIF_DELETE_KEY)!!

        mPifDataPreference.setOnPreferenceClickListener {
            openPifFileSelector()
            true
        }

        mPifDeletePreference.setOnPreferenceClickListener {
            deletePifData()
            true
        }

        updatePifSummaries()

        mMicCameraPrivacy = findPreference("mic_camera_privacy_indicators")
        mLocationPrivacy = findPreference("location_privacy_indicator")

        mMicCameraPrivacy?.apply {
            isChecked = Settings.Secure.getInt(
                requireContext().contentResolver,
                Settings.Secure.MIC_CAMERA_PRIVACY_INDICATORS_ENABLED,
                1
            ) == 1
            setOnPreferenceChangeListener(this@Misc)
        }

        mLocationPrivacy?.apply {
            isChecked = Settings.Secure.getInt(
                requireContext().contentResolver,
                Settings.Secure.LOCATION_PRIVACY_INDICATOR_ENABLED,
                1
            ) == 1
            setOnPreferenceChangeListener(this@Misc)
        }
    }

    private fun openKeyboxFileSelector() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "text/xml"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        mKeyboxFilePickerLauncher.launch(intent)
    }

    private fun openPifFileSelector() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/json"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        mPifFilePickerLauncher.launch(intent)
    }

    private fun deleteKeyboxData() {
        Settings.Secure.putString(
            requireContext().contentResolver,
            Settings.Secure.KEYBOX_DATA,
            null
        )
        Settings.Secure.putString(
            requireContext().contentResolver,
            Settings.Secure.KEYBOX_DATA_TIMESTAMP,
            null
        )
        updateKeyboxSummaries()
        Toast.makeText(context, R.string.keybox_data_cleared, Toast.LENGTH_SHORT).show()
    }

    private fun deletePifData() {
        Settings.Secure.putString(
            requireContext().contentResolver,
            Settings.Secure.PIF_DATA,
            null
        )
        Settings.Secure.putString(
            requireContext().contentResolver,
            Settings.Secure.PIF_DATA_TIMESTAMP,
            null
        )
        updatePifSummaries()
        Toast.makeText(context, R.string.pif_data_cleared, Toast.LENGTH_SHORT).show()
        killPackages()
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
                        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date())
                        
                        Settings.Secure.putString(
                            requireContext().contentResolver,
                            Settings.Secure.KEYBOX_DATA,
                            xml
                        )
                        Settings.Secure.putString(
                            requireContext().contentResolver,
                            Settings.Secure.KEYBOX_DATA_TIMESTAMP,
                            timestamp
                        )
                        updateKeyboxSummaries()
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

    private fun loadPifFile(uri: Uri) {
        Log.d(TAG, "Loading PIF JSON file from URI: ${uri.toString()}")
        
        if (uri.toString().endsWith(".json") || 
            "application/json" == requireContext().contentResolver.getType(uri)) {
            try {
                requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val jsonContent = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        jsonContent.append(line).append('\n')
                    }

                    val json = jsonContent.toString()
                    if (validatePifJson(json)) {
                        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date())
                        
                        Settings.Secure.putString(
                            requireContext().contentResolver,
                            Settings.Secure.PIF_DATA,
                            json
                        )
                        Settings.Secure.putString(
                            requireContext().contentResolver,
                            Settings.Secure.PIF_DATA_TIMESTAMP,
                            timestamp
                        )
                        updatePifSummaries()
                        Toast.makeText(context, R.string.pif_data_loaded, Toast.LENGTH_SHORT).show()
                        killPackages()
                    } else {
                        Toast.makeText(context, R.string.pif_data_invalid, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read PIF JSON file", e)
                Toast.makeText(context, R.string.pif_data_error, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, R.string.pif_data_invalid, Toast.LENGTH_SHORT).show()
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
                                try {
                                    numberOfKeyboxes = parser.text.trim().toInt()
                                } catch (e: NumberFormatException) {
                                    numberOfKeyboxes = -1
                                }
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
               ecdsaCertCount >= 1 && rsaCertCount >= 1
    }

    private fun validatePifJson(json: String): Boolean {
        try {
            val jsonObject = org.json.JSONObject(json)
            // Basic check for a valid JSON object
            if (jsonObject.length() > 0) {
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "JSON validation failed", e)
        }
        return false
    }

    private fun killPackages() {
        try {
            val am = requireContext().getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val packages = arrayOf("com.google.android.gms", "com.android.vending")
            for (pkg in packages) {
                am.javaClass
                  .getMethod("forceStopPackage", String::class.java)
                  .invoke(am, pkg)
                Log.i(TAG, "$pkg process killed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to kill packages", e)
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        when (preference.key) {
            "mic_camera_privacy_indicators" -> {
                Settings.Secure.putInt(
                    requireContext().contentResolver,
                    Settings.Secure.MIC_CAMERA_PRIVACY_INDICATORS_ENABLED,
                    if (newValue as Boolean) 1 else 0
                )
                return true
            }
            "location_privacy_indicator" -> {
                Settings.Secure.putInt(
                    requireContext().contentResolver,
                    Settings.Secure.LOCATION_PRIVACY_INDICATOR_ENABLED,
                    if (newValue as Boolean) 1 else 0
                )
                return true
            }
        }
        return false
    }

    override fun getMetricsCategory(): Int = MetricsEvent.DERPFEST

    companion object {
        const val TAG = "DerpFestCustomizations"
    }
}
