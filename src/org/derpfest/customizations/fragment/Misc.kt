/*
 * SPDX-FileCopyrightText: Project Fluid
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment

class Misc : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {
    private var shouldRecreateOnExit: Boolean = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.misc)

        // Recreate Settings when dashboard icon style changes, but only after leaving this page
        findPreference<Preference>("theming_settings_dashboard_icons")?.onPreferenceChangeListener =
            this
        findPreference<SwitchPreferenceCompat>(KEY_ACCESSCARD)?.onPreferenceChangeListener = this
        updateAccessCardPreference()
    }

    override fun onResume() {
        super.onResume()
        updateAccessCardPreference()
    }

    private fun updateAccessCardPreference() {
        val packages = requireContext().packageManager
        val application =
            try {
                packages.getApplicationInfo(
                    ACCESSCARD_PACKAGE,
                    PackageManager.MATCH_DISABLED_COMPONENTS,
                )
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        val available = application != null && packages.hasSystemFeature(PackageManager.FEATURE_NFC)
        findPreference<PreferenceCategory>("experimental_category")?.isVisible = available
        findPreference<SwitchPreferenceCompat>(KEY_ACCESSCARD)?.apply {
            isEnabled = available
            isChecked = application?.enabled == true
        }
    }

    private fun setAccessCardEnabled(enabled: Boolean): Boolean {
        val packages = requireContext().packageManager
        return try {
            // Package state is per user and persistent. Keep card data when disabling.
            packages.setApplicationEnabledSetting(
                ACCESSCARD_PACKAGE,
                if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
                0,
            )
            val actual =
                packages
                    .getApplicationInfo(
                        ACCESSCARD_PACKAGE,
                        PackageManager.MATCH_DISABLED_COMPONENTS,
                    )
                    .enabled
            if (actual == enabled) true
            else {
                showAccessCardError()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to change AccessCard package state", e)
            showAccessCardError()
            false
        }
    }

    private fun showAccessCardError() {
        Toast.makeText(requireContext(), R.string.accesscard_toggle_error, Toast.LENGTH_LONG).show()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        if (preference.key == KEY_ACCESSCARD) return setAccessCardEnabled(newValue == true)
        if (preference.key == "theming_settings_dashboard_icons") {
            shouldRecreateOnExit = true
        }
        return true
    }

    override fun getMetricsCategory(): Int = MetricsEvent.DERPFEST

    override fun onStop() {
        super.onStop()
        if (shouldRecreateOnExit && isAdded) {
            requireActivity().recreate()
            shouldRecreateOnExit = false
        }
    }

    companion object {
        const val TAG = "DerpFestCustomizations"
        private const val KEY_ACCESSCARD = "accesscard_enabled"
        private const val ACCESSCARD_PACKAGE = "org.derpfest.accesscard"
    }
}
