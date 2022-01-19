/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.internal.util.derp.derpUtils
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable

@SearchIndexable
class Notifications : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.notifications)

        if (!derpUtils.deviceHasFlashlight(requireContext())) {
            findPreference<PreferenceCategory>(FLASHLIGHT_CATEGORY)?.let {
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

        private const val FLASHLIGHT_CATEGORY = "flashlight_category"
        private const val FLASHLIGHT_CALL_PREF = "flashlight_on_call"

        @JvmStatic
        fun reset(context: Context) {
            val resolver: ContentResolver = context.contentResolver
            Settings.System.putIntForUser(
                resolver,
                Settings.System.FLASHLIGHT_ON_CALL,
                0,
                UserHandle.USER_CURRENT,
            )
        }

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            object : BaseSearchIndexProvider(R.xml.notifications) {
                override fun getNonIndexableKeys(context: Context): MutableList<String> {
                    val keys = super.getNonIndexableKeys(context)
                    if (!derpUtils.deviceHasFlashlight(context)) {
                        keys.add(FLASHLIGHT_CALL_PREF)
                    }
                    return keys
                }
            }
    }
}
