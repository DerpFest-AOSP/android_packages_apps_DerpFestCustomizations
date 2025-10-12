/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import com.android.internal.logging.nano.MetricsProto.MetricsEvent

import android.os.Bundle

import androidx.preference.Preference
import androidx.preference.PreferenceCategory

import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment

class About : SettingsPreferenceFragment() {

    private var mDeviceLinks: PreferenceCategory? = null
    private var mDeviceFW: Preference? = null
    private var mDeviceRecovery: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.about)

        val res = resources

        mDeviceLinks = findPreference("about_device_links")
        mDeviceFW = findPreference("device_fw")
        mDeviceRecovery = findPreference("device_recovery")

        val hasFWLink = res.getString(R.string.about_device_fw_link).isNotEmpty()
        if (!hasFWLink) {
            mDeviceFW?.isVisible = false
        }

        val hasRecoveryLink = res.getString(R.string.about_device_recovery_link).isNotEmpty()
        if (!hasRecoveryLink) {
            mDeviceRecovery?.isVisible = false
        }

        if (!hasFWLink && !hasRecoveryLink) {
            mDeviceLinks?.isVisible = false
        }
    }

    override fun getMetricsCategory(): Int = MetricsEvent.DERPFEST

    companion object {
        const val TAG = "DerpFestCustomizations"
    }
}

