/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import android.content.Context
import android.os.Bundle

import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable

@SearchIndexable
class EdgeLightSettings : SettingsPreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.edge_light_settings)
    }

    override fun getMetricsCategory(): Int = MetricsEvent.DERPFEST

    companion object {
        @JvmField
        val TAG: String = "EdgeLightSettings"

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            object : BaseSearchIndexProvider(R.xml.edge_light_settings) {
                override fun getNonIndexableKeys(context: Context): List<String> =
                    super.getNonIndexableKeys(context)
            }
    }
}
