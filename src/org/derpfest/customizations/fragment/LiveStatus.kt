/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import android.content.Context
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

        if (!DeviceUtils.hasCenteredCutout(requireContext())) {
            findPreference<Preference>("status_bar_dynamic_island")?.let {
                preferenceScreen.removePreference(it)
            }
        }
    }

    override fun getMetricsCategory(): Int = MetricsEvent.DERPFEST

    companion object {
        const val TAG = "DerpFestCustomizations"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            object : BaseSearchIndexProvider(R.xml.live_status) {
                override fun getNonIndexableKeys(context: Context): MutableList<String> {
                    val keys = super.getNonIndexableKeys(context)
                    if (!DeviceUtils.hasCenteredCutout(context)) {
                        keys.add("status_bar_dynamic_island")
                    }
                    return keys
                }
            }
    }
}
