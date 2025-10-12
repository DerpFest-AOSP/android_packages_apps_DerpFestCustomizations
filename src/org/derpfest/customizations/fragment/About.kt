/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import com.android.internal.logging.nano.MetricsProto.MetricsEvent

import android.os.Bundle

import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment

class About : SettingsPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.about)
    }

    override fun getMetricsCategory(): Int = MetricsEvent.DERPFEST

    companion object {
        const val TAG = "DerpFestCustomizations"
    }
}

