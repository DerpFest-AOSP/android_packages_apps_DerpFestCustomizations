/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import android.provider.Settings
import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.Preference.OnPreferenceChangeListener
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment

class GradientSettings : SettingsPreferenceFragment(), OnPreferenceChangeListener {

    private var gradientColorsCategory: PreferenceCategory? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.gradient_settings)
        gradientColorsCategory = findPreference("gradient_colors_category")
        findPreference<Preference>("qs_tile_gradient_enabled")?.setOnPreferenceChangeListener(this)
        findPreference<Preference>("qs_brightness_gradient_enabled")?.setOnPreferenceChangeListener(this)
        findPreference<Preference>("qs_volume_gradient_enabled")?.setOnPreferenceChangeListener(this)
    }

    override fun onResume() {
        super.onResume()
        updateColorPickersAvailability()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        updateColorPickersAvailability(changedKey = preference.key, newValue = newValue)
        return true
    }

    /** True when at least one of the three gradient toggles is enabled. */
    private fun isAnyGradientEnabled(): Boolean {
        val cr = requireContext().contentResolver
        val tile = Settings.System.getInt(cr, "qs_tile_gradient_enabled", 1) != 0
        val brightness = Settings.System.getInt(cr, "qs_brightness_gradient_enabled", 1) != 0
        val volume = Settings.System.getInt(cr, "qs_volume_gradient_enabled", 1) != 0
        return tile || brightness || volume
    }

    /**
     * Updates gradient start/end availability. When called from onPreferenceChange, the changed
     * preference is not yet written to Settings, so we pass [changedKey] and [newValue] to use
     * the new value for that toggle and avoid wrong availability (e.g. needing two toggles to
     * enable, or category staying enabled when all are turned off).
     */
    private fun updateColorPickersAvailability(changedKey: String? = null, newValue: Any? = null) {
        val cr = requireContext().contentResolver
        val tile = if (changedKey == "qs_tile_gradient_enabled") {
            newValue as? Boolean ?: (Settings.System.getInt(cr, "qs_tile_gradient_enabled", 1) != 0)
        } else {
            Settings.System.getInt(cr, "qs_tile_gradient_enabled", 1) != 0
        }
        val brightness = if (changedKey == "qs_brightness_gradient_enabled") {
            newValue as? Boolean ?: (Settings.System.getInt(cr, "qs_brightness_gradient_enabled", 1) != 0)
        } else {
            Settings.System.getInt(cr, "qs_brightness_gradient_enabled", 1) != 0
        }
        val volume = if (changedKey == "qs_volume_gradient_enabled") {
            newValue as? Boolean ?: (Settings.System.getInt(cr, "qs_volume_gradient_enabled", 1) != 0)
        } else {
            Settings.System.getInt(cr, "qs_volume_gradient_enabled", 1) != 0
        }
        gradientColorsCategory?.isEnabled = tile || brightness || volume
    }

    override fun getMetricsCategory(): Int = MetricsEvent.DERPFEST

    companion object {
        const val TAG = "GradientSettings"
    }
}
