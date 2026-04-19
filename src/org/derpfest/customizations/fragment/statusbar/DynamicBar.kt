/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-FileCopyrightText: AxionOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment.statusbar

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import org.json.JSONArray

class DynamicBar : SettingsPreferenceFragment() {

    private val resolver: ContentResolver
        get() = requireContext().contentResolver

    private val handler = Handler(Looper.getMainLooper())
    private var settingsObserver: ContentObserver? = null

    private val eventTypeIds = listOf(
        "audio_recording",
        "media",
        "call",
        "notification",
        "timer",
        "stopwatch",
        "alarm",
        "charging",
        "bluetooth",
        "hotspot",
        "ringer",
        "vpn",
        "clipboard",
        "torch",
        "promoted_ongoing",
        "sports",
        "app_switch",
        "biometric_unlock",
    )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.dynamic_bar)

        setupKeyguardSubPrefs()
        setupEventToggles()
        registerObserver()
    }

    override fun onDestroy() {
        super.onDestroy()
        settingsObserver?.let { resolver.unregisterContentObserver(it) }
    }

    private fun setupKeyguardSubPrefs() {
        val keyguardPref = findPreference<SwitchPreferenceCompat>(SETTINGS_KEY_KEYGUARD_ENABLED)
        keyguardPref?.setOnPreferenceChangeListener { _, newValue ->
            updateKeyguardSubPrefsVisibility(newValue as Boolean)
            true
        }
        val keyguardEnabled = Settings.Secure.getIntForUser(
            resolver, SETTINGS_KEY_KEYGUARD_ENABLED, 1, UserHandle.USER_CURRENT
        ) == 1
        updateKeyguardSubPrefsVisibility(keyguardEnabled)
    }

    private fun updateKeyguardSubPrefsVisibility(keyguardEnabled: Boolean) {
        findPreference<Preference>(SETTINGS_KEY_BATTERY_CHIP_MODE)?.isVisible = keyguardEnabled
    }

    private fun setupEventToggles() {
        val disabledEvents = getDisabledEvents()

        for (typeId in eventTypeIds) {
            val pref = findPreference<SwitchPreferenceCompat>("event_$typeId") ?: continue
            pref.isChecked = typeId !in disabledEvents
            pref.setOnPreferenceChangeListener { _, newValue ->
                toggleEvent(typeId, newValue as Boolean)
                true
            }
        }
    }

    private fun getDisabledEvents(): Set<String> {
        val json = Settings.Secure.getStringForUser(
            resolver, SETTINGS_KEY_EVENTS, UserHandle.USER_CURRENT
        ) ?: return emptySet()

        if (json.isBlank()) return emptySet()

        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapTo(mutableSetOf()) { arr.getString(it) }
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun toggleEvent(typeId: String, enabled: Boolean) {
        val current = getDisabledEvents()
        val updated = if (enabled) current - typeId else current + typeId
        val json = if (updated.isEmpty()) "" else JSONArray(updated.toList()).toString()
        Settings.Secure.putStringForUser(
            resolver, SETTINGS_KEY_EVENTS, json, UserHandle.USER_CURRENT
        )
    }

    private fun registerObserver() {
        settingsObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                when (uri?.lastPathSegment) {
                    SETTINGS_KEY_EVENTS -> {
                        val disabledEvents = getDisabledEvents()
                        for (typeId in eventTypeIds) {
                            val pref = findPreference<SwitchPreferenceCompat>("event_$typeId")
                            pref?.isChecked = typeId !in disabledEvents
                        }
                    }
                    SETTINGS_KEY_KEYGUARD_ENABLED -> {
                        val enabled = Settings.Secure.getIntForUser(
                            resolver, SETTINGS_KEY_KEYGUARD_ENABLED, 1, UserHandle.USER_CURRENT
                        ) == 1
                        updateKeyguardSubPrefsVisibility(enabled)
                    }
                }
            }
        }

        resolver.registerContentObserver(
            Settings.Secure.getUriFor(SETTINGS_KEY_EVENTS),
            false, settingsObserver!!
        )
        resolver.registerContentObserver(
            Settings.Secure.getUriFor(SETTINGS_KEY_KEYGUARD_ENABLED),
            false, settingsObserver!!
        )
    }

    override fun getMetricsCategory(): Int {
        return MetricsProto.MetricsEvent.DERPFEST
    }

    companion object {
        private const val SETTINGS_KEY_ENABLED = "ax_dynamic_bar_enabled"
        private const val SETTINGS_KEY_KEYGUARD_ENABLED = "ax_dynamic_bar_keyguard_enabled"
        private const val SETTINGS_KEY_EVENTS = "ax_dynamic_bar_events"
        private const val SETTINGS_KEY_BATTERY_CHIP_MODE = "ax_dynamic_bar_keyguard_battery_chip_mode"

        @JvmStatic
        fun reset(context: Context) {
            val resolver = context.contentResolver
            Settings.Secure.putIntForUser(
                resolver, SETTINGS_KEY_ENABLED, 0,
                UserHandle.USER_CURRENT
            )
            Settings.Secure.putIntForUser(
                resolver, SETTINGS_KEY_KEYGUARD_ENABLED, 1,
                UserHandle.USER_CURRENT
            )
            Settings.Secure.putIntForUser(
                resolver, SETTINGS_KEY_BATTERY_CHIP_MODE, 1,
                UserHandle.USER_CURRENT
            )
        }
    }
}
