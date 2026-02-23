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
        updateColorPickersAvailability()
        return true
    }

    private fun isAnyGradientEnabled(): Boolean {
        val cr = requireContext().contentResolver
        val tile = Settings.System.getInt(cr, "qs_tile_gradient_enabled", 1) != 0
        val brightness = Settings.System.getInt(cr, "qs_brightness_gradient_enabled", 1) != 0
        val volume = Settings.System.getInt(cr, "qs_volume_gradient_enabled", 1) != 0
        return tile || brightness || volume
    }

    private fun updateColorPickersAvailability() {
        gradientColorsCategory?.isEnabled = isAnyGradientEnabled()
    }

    override fun getMetricsCategory(): Int = MetricsEvent.DERPFEST

    companion object {
        const val TAG = "GradientSettings"
    }
}
