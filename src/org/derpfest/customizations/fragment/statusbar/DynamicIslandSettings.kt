/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment.statusbar

import android.content.Context
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable
import org.derpfest.customizations.utils.DeviceUtils

@SearchIndexable
class DynamicIslandSettings : SettingsPreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.dynamic_island_settings)
    }

    override fun getMetricsCategory(): Int = MetricsProto.MetricsEvent.DERPFEST

    companion object {
        @JvmStatic
        fun reset(context: Context) {
            val resolver = context.contentResolver
            Settings.System.putIntForUser(
                resolver,
                Settings.System.STATUS_BAR_SHOW_DYNAMIC_ISLAND,
                0,
                UserHandle.USER_CURRENT,
            )
            Settings.System.putIntForUser(
                resolver,
                Settings.System.STATUS_BAR_DYNAMIC_ISLAND_MEDIA_CONTROLS,
                1,
                UserHandle.USER_CURRENT,
            )
            Settings.System.putIntForUser(
                resolver,
                Settings.System.STATUS_BAR_DYNAMIC_ISLAND_LYRICS,
                0,
                UserHandle.USER_CURRENT,
            )
            Settings.System.putIntForUser(
                resolver,
                Settings.System.STATUS_BAR_DYNAMIC_ISLAND_ONGOING_ACTIVITIES,
                1,
                UserHandle.USER_CURRENT,
            )
            Settings.System.putIntForUser(
                resolver,
                Settings.System.STATUS_BAR_DYNAMIC_ISLAND_CALLS,
                1,
                UserHandle.USER_CURRENT,
            )
            Settings.System.putIntForUser(
                resolver,
                Settings.System.STATUS_BAR_DYNAMIC_ISLAND_SCREEN_RECORDING,
                1,
                UserHandle.USER_CURRENT,
            )
            Settings.System.putIntForUser(
                resolver,
                Settings.System.STATUS_BAR_DYNAMIC_ISLAND_ALARMS,
                1,
                UserHandle.USER_CURRENT,
            )
            Settings.System.putIntForUser(
                resolver,
                Settings.System.STATUS_BAR_DYNAMIC_ISLAND_STOPWATCH,
                1,
                UserHandle.USER_CURRENT,
            )
            Settings.System.putIntForUser(
                resolver,
                Settings.System.STATUS_BAR_DYNAMIC_ISLAND_FLASHLIGHT,
                1,
                UserHandle.USER_CURRENT,
            )
            Settings.System.putIntForUser(
                resolver,
                Settings.System.STATUS_BAR_DYNAMIC_ISLAND_LIVE_SCORES,
                1,
                UserHandle.USER_CURRENT,
            )
            Settings.System.putIntForUser(
                resolver,
                Settings.System.STATUS_BAR_DYNAMIC_ISLAND_WIDTH,
                110,
                UserHandle.USER_CURRENT,
            )
        }

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            object : BaseSearchIndexProvider(R.xml.dynamic_island_settings) {
                override fun getNonIndexableKeys(context: Context): MutableList<String> {
                    val keys = super.getNonIndexableKeys(context)
                    if (!DeviceUtils.hasCenteredCutout(context)) {
                        keys.add("dynamic_island_settings")
                    }
                    return keys
                }
            }
    }
}
