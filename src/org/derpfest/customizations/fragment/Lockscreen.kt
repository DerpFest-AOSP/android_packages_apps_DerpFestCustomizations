/*
 * SPDX-FileCopyrightText: Project Fluid
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import com.android.internal.logging.nano.MetricsProto.MetricsEvent

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings

import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat

import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment

import org.derpfest.customizations.utils.DeviceUtils

class Lockscreen : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    private val KEY_FP_SUCCESS = "fp_success_vibrate"
    private val KEY_FP_ERROR = "fp_error_vibrate"
    private val KEY_AUTH_RIPPLE = "auth_ripple_enabled"
    private val GENERAL_CATEGORY = "general_category"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.lockscreen)

        val context = requireContext()
        val generalCategory = findPreference<PreferenceCategory>(GENERAL_CATEGORY)
        val fpSuccessVib = findPreference<Preference>(KEY_FP_SUCCESS)
        val fpErrorVib = findPreference<Preference>(KEY_FP_ERROR)
        val authRipple = findPreference<Preference>(KEY_AUTH_RIPPLE)

        val hasFingerprint = DeviceUtils.hasFingerprint(context)
        val hapticAvailable = DeviceUtils.hasVibrator(context)

        // Remove fingerprint vibration preferences if device doesn't have fingerprint or vibrator
        if (!hasFingerprint || !hapticAvailable) {
            generalCategory?.let {
                fpSuccessVib?.let { pref -> it.removePreference(pref) }
                fpErrorVib?.let { pref -> it.removePreference(pref) }
            }
        }

        // Remove ripple effect preference if device doesn't have fingerprint
        if (!hasFingerprint) {
            generalCategory?.let {
                authRipple?.let { pref -> it.removePreference(pref) }
            }
        }

        findPreference<SwitchPreferenceCompat>(KEY_NOW_PLAYING)?.apply {
            isChecked = isSettingEnabled(KEY_NOW_PLAYING, usesGoogleAmbient())
            setOnPreferenceChangeListener(this@Lockscreen)
        }
        findPreference<SwitchPreferenceCompat>(KEY_NOW_PLAYING_ON_DEMAND)?.apply {
            isChecked = isSettingEnabled(KEY_NOW_PLAYING_ON_DEMAND, secure = false)
            setOnPreferenceChangeListener(this@Lockscreen)
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val enabled = newValue == true
        when (preference.key) {
            KEY_NOW_PLAYING -> setNowPlayingEnabled(enabled)
            KEY_NOW_PLAYING_ON_DEMAND -> setSystemSetting(KEY_NOW_PLAYING_ON_DEMAND, enabled)
        }
        return true
    }

    /** Pixels with Android System Intelligence listen through that package, not SystemUI. */
    private fun usesGoogleAmbient(): Boolean {
        val isPixel = Build.BRAND.equals("google", ignoreCase = true) ||
            Build.MANUFACTURER.equals("google", ignoreCase = true)
        if (!isPixel) {
            return false
        }
        return try {
            requireContext().packageManager.getPackageInfo(GOOGLE_AMBIENT_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun isSettingEnabled(key: String, secure: Boolean): Boolean {
        val resolver = requireContext().contentResolver
        val value = if (secure) {
            Settings.Secure.getInt(resolver, key, 0)
        } else {
            Settings.System.getInt(resolver, key, 0)
        }
        return value == 1
    }

    private fun setSystemSetting(key: String, enabled: Boolean) {
        Settings.System.putInt(
            requireContext().contentResolver,
            key,
            if (enabled) 1 else 0,
        )
    }

    private fun setNowPlayingEnabled(enabled: Boolean) {
        val resolver = requireContext().contentResolver
        val value = if (enabled) 1 else 0
        // SystemUI's own recognizer reads the system setting. Ambient Services on a
        // Pixel reads the secure setting. Write both so the switch covers either device.
        Settings.System.putInt(resolver, KEY_NOW_PLAYING, value)
        Settings.Secure.putInt(resolver, KEY_NOW_PLAYING, value)
    }

    override fun getMetricsCategory(): Int = MetricsEvent.DERPFEST

    companion object {
        const val TAG = "DerpFestCustomizations"
        private const val KEY_NOW_PLAYING = "now_playing_enabled"
        private const val KEY_NOW_PLAYING_ON_DEMAND = "now_playing_on_demand"
        private const val GOOGLE_AMBIENT_PACKAGE = "com.google.android.as"
    }
}
