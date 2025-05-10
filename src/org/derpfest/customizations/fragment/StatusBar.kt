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
import androidx.preference.SwitchPreferenceCompat
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment

class StatusBar : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    private var mMicCameraPrivacy: SwitchPreferenceCompat? = null
    private var mLocationPrivacy: SwitchPreferenceCompat? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.statusbar)

        mMicCameraPrivacy = findPreference("mic_camera_privacy_indicators")
        mLocationPrivacy = findPreference("location_privacy_indicator")

        mMicCameraPrivacy?.apply {
            isChecked = Settings.Secure.getInt(
                requireContext().contentResolver,
                Settings.Secure.MIC_CAMERA_PRIVACY_INDICATORS_ENABLED,
                1
            ) == 1
            setOnPreferenceChangeListener(this@StatusBar)
        }

        mLocationPrivacy?.apply {
            isChecked = Settings.Secure.getInt(
                requireContext().contentResolver,
                Settings.Secure.LOCATION_PRIVACY_INDICATOR_ENABLED,
                1
            ) == 1
            setOnPreferenceChangeListener(this@StatusBar)
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
