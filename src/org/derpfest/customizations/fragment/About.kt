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
import android.widget.Toast

import androidx.preference.Preference
import androidx.preference.PreferenceCategory

import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settingslib.widget.LayoutPreference

class About : SettingsPreferenceFragment(), Preference.OnPreferenceClickListener {

    private var mDerpLogo: LayoutPreference? = null
    private var mDeviceLinks: PreferenceCategory? = null
    private var mDeviceFW: Preference? = null
    private var mDeviceRecovery: Preference? = null
    private var mDerpHitToast: Toast? = null

    private var mDerpLogoHitCountdown = 10

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

        mDerpLogo = findPreference("derp_logo")
        mDerpLogo?.onPreferenceClickListener = this
        mDerpHitToast = null
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        if (mDerpLogoHitCountdown > 0) {
            mDerpLogoHitCountdown--
            if (mDerpLogoHitCountdown == 0) {
                mDerpHitToast?.cancel()
                mDerpHitToast = Toast.makeText(
                    context,
                    resources.getString(R.string.derpd_done),
                    Toast.LENGTH_SHORT
                )
                mDerpHitToast?.show()
                throw RuntimeException("BOOYAH!") // Crash :)
            } else {
                mDerpHitToast?.cancel()
                mDerpHitToast = Toast.makeText(
                    context,
                    resources.getQuantityString(
                        R.plurals.show_derped_countdown,
                        mDerpLogoHitCountdown,
                        mDerpLogoHitCountdown
                    ),
                    Toast.LENGTH_SHORT
                )
                mDerpHitToast?.show()
            }
        }
        return true
    }

    override fun getMetricsCategory(): Int = MetricsEvent.DERPFEST

    companion object {
        const val TAG = "DerpFestCustomizations"
    }
}

