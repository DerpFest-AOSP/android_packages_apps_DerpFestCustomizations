/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import android.os.Bundle
import com.android.internal.logging.nano.MetricsProto
import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settingslib.search.SearchIndexable
import com.android.settings.search.BaseSearchIndexProvider

@SearchIndexable
class Ticker : SettingsPreferenceFragment() {

    private val STATUSBAR_TICKER_FOOTER = "statusbar_ticker_footer"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.statusbar_ticker)
        findPreference<com.android.settingslib.widget.FooterPreference>(STATUSBAR_TICKER_FOOTER)?.setTitle(R.string.ticker_screen_footer)
    }

    override fun getMetricsCategory(): Int = MetricsProto.MetricsEvent.DERPFEST

    companion object {
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER = BaseSearchIndexProvider(R.xml.statusbar_ticker)
    }
}

