/*
 * SPDX-FileCopyrightText: Project Fluid
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import com.android.internal.logging.nano.MetricsProto.MetricsEvent

import android.os.Bundle

import androidx.preference.Preference

import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment

class Misc : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {
    private var shouldRecreateOnExit: Boolean = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.misc)

        // Recreate Settings when dashboard icon style changes, but only after leaving this page
        findPreference<Preference>("theming_settings_dashboard_icons")?.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
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
    }
}
