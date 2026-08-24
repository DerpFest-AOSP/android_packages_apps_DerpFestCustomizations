/*
 * SPDX-FileCopyrightText: Project Fluid
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
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        return true
    }

    override fun getMetricsCategory(): Int = MetricsEvent.DERPFEST

    companion object {
        const val TAG = "DerpFestCustomizations"
    }
}
