/*
 * SPDX-FileCopyrightText: Project Fluid
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import com.android.internal.logging.nano.MetricsProto.MetricsEvent

import android.os.Bundle
import android.provider.Settings

import androidx.preference.Preference
import org.derpfest.support.preferences.SecureSettingSeekBarPreference
import org.derpfest.support.preferences.SecureSettingSwitchPreference
import androidx.preference.ListPreference

import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment

class QS : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    private lateinit var mDataUsagePreference: Preference
    private lateinit var mDataUsageCycleTypePreference: ListPreference
    private lateinit var mNotifTransparencyLevelPref: SecureSettingSeekBarPreference
    private lateinit var mNotifTransparencyContextPref: SecureSettingSwitchPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.qs)
        
        mDataUsagePreference = findPreference("qs_show_data_usage")!!
        mDataUsageCycleTypePreference = findPreference("qs_data_usage_cycle_type")!!
        mNotifTransparencyLevelPref = findPreference("notification_row_transparency_level")!!
        mNotifTransparencyContextPref = findPreference("notification_row_transparency_context_aware")!!
        
        mDataUsageCycleTypePreference.setOnPreferenceChangeListener(this)
        mNotifTransparencyLevelPref.setOnPreferenceChangeListener(this)
        
        updateDataUsageSummary()

        // Initialize context-aware toggle availability based on current slider value
        val currentLevel = Settings.Secure.getInt(
            requireContext().contentResolver,
            "notification_row_transparency_level",
            15
        )
        updateTransparencyContextAvailability(currentLevel)
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        when (preference.key) {
            "qs_data_usage_cycle_type" -> {
                updateDataUsageSummary(newValue as? String)
                return true
            }
            "notification_row_transparency_level" -> {
                val level = (newValue as? Int) ?: return false
                updateTransparencyContextAvailability(level)
                return true
            }
        }
        return true
    }

    private fun updateDataUsageSummary(cycleTypeValue: String? = null) {
        val cycleType = cycleTypeValue?.toIntOrNull() ?: Settings.Secure.getInt(
            requireContext().contentResolver,
            "qs_data_usage_cycle_type",
            0
        )
        
        val summaryResId = when (cycleType) {
            0 -> R.string.qs_footer_datausage_summary_daily
            1 -> R.string.qs_footer_datausage_summary_weekly
            else -> R.string.qs_footer_datausage_summary_daily
        }
        
        mDataUsagePreference.summary = getString(summaryResId)
    }

    private fun updateTransparencyContextAvailability(level: Int) {
        // Disable context-aware toggle when transparency is 0 (AOSP default opaque)
        mNotifTransparencyContextPref.isEnabled = level > 0
    }

    override fun getMetricsCategory(): Int = MetricsEvent.DERPFEST

    companion object {
        const val TAG = "DerpFestCustomizations"
    }
}
