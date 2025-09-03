/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import com.android.internal.logging.nano.MetricsProto.MetricsEvent

import android.os.Bundle
import android.provider.Settings

import androidx.preference.Preference
import org.derpfest.support.preferences.ProperSeekBarPreference

import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment

class QsLayoutSettings : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    private lateinit var mQsColumnsPreference: ProperSeekBarPreference
    private lateinit var mQsRowsPreference: ProperSeekBarPreference
    private lateinit var mQsColumnsLandscapePreference: ProperSeekBarPreference
    private lateinit var mQsRowsLandscapePreference: ProperSeekBarPreference
    private lateinit var mQqsRowsPreference: ProperSeekBarPreference
    private lateinit var mQqsRowsLandscapePreference: ProperSeekBarPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.qs_layout_settings)
        
        mQsColumnsPreference = findPreference("qs_tiles_columns")!!
        mQsRowsPreference = findPreference("qs_tiles_rows")!!
        mQsColumnsLandscapePreference = findPreference("qs_tiles_columns_landscape")!!
        mQsRowsLandscapePreference = findPreference("qs_tiles_rows_landscape")!!
        mQqsRowsPreference = findPreference("qqs_tiles_rows")!!
        mQqsRowsLandscapePreference = findPreference("qqs_tiles_rows_landscape")!!
        
        mQsColumnsPreference.setOnPreferenceChangeListener(this)
        mQsRowsPreference.setOnPreferenceChangeListener(this)
        mQsColumnsLandscapePreference.setOnPreferenceChangeListener(this)
        mQsRowsLandscapePreference.setOnPreferenceChangeListener(this)
        mQqsRowsPreference.setOnPreferenceChangeListener(this)
        mQqsRowsLandscapePreference.setOnPreferenceChangeListener(this)
        
        // Set initial values from system settings
        setInitialValues()
    }

    override fun onResume() {
        super.onResume()
        // Refresh values when returning to the page
        setInitialValues()
    }

    private fun setInitialValues() {
        // Load current values from system settings and set them in preferences
        val columnsValue = Settings.System.getInt(
            requireContext().contentResolver,
            "qs_tiles_columns",
            4
        )
        val rowsValue = Settings.System.getInt(
            requireContext().contentResolver,
            "qs_tiles_rows",
            4
        )
        val qqsRowsValue = Settings.System.getInt(
            requireContext().contentResolver,
            "qqs_tiles_rows",
            2
        )
        val columnsLandscapeValue = Settings.System.getInt(
            requireContext().contentResolver,
            "qs_tiles_columns_landscape",
            4
        )
        val rowsLandscapeValue = Settings.System.getInt(
            requireContext().contentResolver,
            "qs_tiles_rows_landscape",
            2
        )
        val qqsRowsLandscapeValue = Settings.System.getInt(
            requireContext().contentResolver,
            "qqs_tiles_rows_landscape",
            1
        )
        
        // Set seekbar values directly from system settings
        mQsColumnsPreference.setValue(columnsValue)
        mQsRowsPreference.setValue(rowsValue)
        mQsColumnsLandscapePreference.setValue(columnsLandscapeValue)
        mQsRowsLandscapePreference.setValue(rowsLandscapeValue)
        mQqsRowsPreference.setValue(qqsRowsValue)
        mQqsRowsLandscapePreference.setValue(qqsRowsLandscapeValue)
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        when (preference.key) {
            "qs_tiles_columns" -> {
                val seekbarValue = newValue as? Int ?: return false
                // Seekbar shows actual values: 2-6 columns
                Settings.System.putInt(requireContext().contentResolver, "qs_tiles_columns", seekbarValue)
                // Ensure the preference shows the correct value
                mQsColumnsPreference.setValue(seekbarValue)
                return true
            }
            "qs_tiles_rows" -> {
                val seekbarValue = newValue as? Int ?: return false
                // Seekbar shows actual values: 2-5 rows
                Settings.System.putInt(requireContext().contentResolver, "qs_tiles_rows", seekbarValue)
                // Ensure the preference shows the correct value
                mQsRowsPreference.setValue(seekbarValue)
                return true
            }
            "qqs_tiles_rows" -> {
                val seekbarValue = newValue as? Int ?: return false
                // Seekbar shows actual values: 1-3 rows
                Settings.System.putInt(requireContext().contentResolver, "qqs_tiles_rows", seekbarValue)
                // Ensure the preference shows the correct value
                mQqsRowsPreference.setValue(seekbarValue)
                return true
            }
            "qs_tiles_columns_landscape" -> {
                val seekbarValue = newValue as? Int ?: return false
                // Seekbar shows actual values: 2-6 columns
                Settings.System.putInt(requireContext().contentResolver, "qs_tiles_columns_landscape", seekbarValue)
                // Ensure the preference shows the correct value
                mQsColumnsLandscapePreference.setValue(seekbarValue)
                return true
            }
            "qs_tiles_rows_landscape" -> {
                val seekbarValue = newValue as? Int ?: return false
                // Seekbar shows actual values: 1-3 rows
                Settings.System.putInt(requireContext().contentResolver, "qs_tiles_rows_landscape", seekbarValue)
                // Ensure the preference shows the correct value
                mQsRowsLandscapePreference.setValue(seekbarValue)
                return true
            }
            "qqs_tiles_rows_landscape" -> {
                val seekbarValue = newValue as? Int ?: return false
                // Seekbar shows actual values: 1-4 rows
                Settings.System.putInt(requireContext().contentResolver, "qqs_tiles_rows_landscape", seekbarValue)
                // Ensure the preference shows the correct value
                mQqsRowsLandscapePreference.setValue(seekbarValue)
                return true
            }
        }
        return true
    }

    override fun getMetricsCategory(): Int = MetricsEvent.DERPFEST

    companion object {
        const val TAG = "QsLayoutSettings"
    }
}
