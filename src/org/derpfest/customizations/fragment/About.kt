/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import com.android.internal.logging.nano.MetricsProto.MetricsEvent

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
        } else {
            var defaultBrowser: String? = null
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://"))
                val resolveInfo = activity?.packageManager?.resolveActivity(
                    browserIntent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )
                defaultBrowser = resolveInfo?.activityInfo?.packageName
            } catch (e: Exception) {
                // nothing to do. defaultBrowser already set to null
            }

            // if we have no default browser set we disable the buttons and let the user know
            if (defaultBrowser == null) {
                mDeviceFW?.isEnabled = false
                mDeviceFW?.summary = res.getString(R.string.no_browser)
                mDeviceRecovery?.isEnabled = false
                mDeviceRecovery?.summary = res.getString(R.string.no_browser)
            }
        }
    }

    override fun getMetricsCategory(): Int = MetricsEvent.DERPFEST

    companion object {
        const val TAG = "DerpFestCustomizations"
    }
}

