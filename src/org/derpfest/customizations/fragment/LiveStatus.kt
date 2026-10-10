/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import android.os.Bundle
import androidx.preference.Preference
import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable
import org.derpfest.customizations.utils.DeviceUtils

@SearchIndexable
class LiveStatus : SettingsPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.live_status)
        updateDynamicIslandAvailability()
    }

    override fun onResume() {
        super.onResume()
        updateDynamicIslandAvailability()
    }

    private fun updateDynamicIslandAvailability() {
        val islandPref = findPreference<Preference>(KEY_DYNAMIC_ISLAND) ?: return
        val hasCutout = DeviceUtils.hasCenteredCutout(requireContext())
        islandPref.isEnabled = hasCutout
        islandPref.summary = if (hasCutout) {
            getString(R.string.status_bar_dynamic_island_summary)
        } else {
            getString(R.string.status_bar_dynamic_island_unavailable)
        }
    }

    override fun getMetricsCategory(): Int = MetricsEvent.DERPFEST

    companion object {
        const val TAG = "DerpFestCustomizations"
        const val KEY_DYNAMIC_ISLAND = "status_bar_dynamic_island"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = BaseSearchIndexProvider(R.xml.live_status)
    }
}
