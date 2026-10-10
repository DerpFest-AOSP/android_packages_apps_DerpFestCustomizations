/*
 * SPDX-FileCopyrightText: Project Fluid
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations

import com.android.internal.logging.nano.MetricsProto.MetricsEvent

import android.os.Bundle

import androidx.preference.Preference
import androidx.appcompat.app.ActionBar

import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import org.derpfest.customizations.utils.DeviceUtils

class DerpFestCustomizations : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.home)
        updateLiveStatusSummary()
    }

    override fun onResume() {
        super.onResume()
        updateLiveStatusSummary()
    }

    private fun updateLiveStatusSummary() {
        val pref = findPreference<Preference>("live_status") ?: return
        pref.summary = if (DeviceUtils.hasCenteredCutout(requireContext())) {
            getString(R.string.live_status_summary)
        } else {
            getString(R.string.live_status_summary_no_cutout)
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
