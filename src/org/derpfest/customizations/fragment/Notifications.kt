/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import android.content.Context
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable
import org.derpfest.customizations.utils.DeviceUtils

@SearchIndexable
class Notifications : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.notifications)

        if (!DeviceUtils.hasCenteredCutout(requireContext())) {
            findPreference<PreferenceCategory>("dynamic_island_category")?.let {
                preferenceScreen.removePreference(it)
            }
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        return true
    }

    override fun getMetricsCategory(): Int = MetricsEvent.DERPFEST

    companion object {
        const val TAG = "DerpFestCustomizations"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            object : BaseSearchIndexProvider(R.xml.notifications) {
                override fun getNonIndexableKeys(context: Context): MutableList<String> {
                    val keys = super.getNonIndexableKeys(context)
                    if (!DeviceUtils.hasCenteredCutout(context)) {
                        keys.add("dynamic_island_category")
                        keys.add("status_bar_dynamic_island")
                    }
                    return keys
                }
            }
    }
}
