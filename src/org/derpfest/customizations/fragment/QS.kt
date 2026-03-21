/*
 * SPDX-FileCopyrightText: Project Fluid
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import com.android.internal.logging.nano.MetricsProto.MetricsEvent

import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings

import androidx.preference.Preference
import androidx.preference.ListPreference

import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment

class QS : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    private lateinit var mDataUsagePreference: Preference
    private lateinit var mDataUsageCycleTypePreference: ListPreference
    private var mTileLabelHide: Preference? = null
    private var mTileIconShape: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.qs)

        findPreference<Preference>(KEY_QS_PANEL_STYLE)?.setOnPreferenceChangeListener(this)
        mTileLabelHide = findPreference(KEY_TILE_LABEL_HIDE)
        mTileIconShape = findPreference(KEY_QS_TILE_ICON_SHAPE)
        val style = Settings.Secure.getIntForUser(
            requireContext().contentResolver,
            KEY_QS_PANEL_STYLE,
            0,
            UserHandle.USER_CURRENT,
        )
        updateCircularPrefs(style == 1)

        mDataUsagePreference = findPreference("qs_show_data_usage")!!
        mDataUsageCycleTypePreference = findPreference("qs_data_usage_cycle_type")!!

        mDataUsageCycleTypePreference.setOnPreferenceChangeListener(this)

        updateDataUsageSummary()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        when (preference.key) {
            KEY_QS_PANEL_STYLE -> {
                val style = (newValue as? String)?.toIntOrNull() ?: 0
                updateCircularPrefs(style == 1)
                return true
            }
            "qs_data_usage_cycle_type" -> {
                updateDataUsageSummary(newValue as? String)
                return true
            }
        }
        return true
    }

    private fun updateCircularPrefs(circular: Boolean) {
        mTileLabelHide?.isVisible = circular
        mTileIconShape?.isVisible = circular
    }

    private fun updateDataUsageSummary(cycleTypeValue: String? = null) {
        val cycleType = cycleTypeValue?.toIntOrNull() ?: Settings.Secure.getInt(
            requireContext().contentResolver,
            "qs_data_usage_cycle_type",
            0
        )
        
        val summaryResId = when (cycleType) {
            0 -> R.string.qs_footer_datausage_summary_daily
            1 -> R.string.qs_footer_datausage_summary_weekly
            else -> R.string.qs_footer_datausage_summary_daily
        }
        
        mDataUsagePreference.summary = getString(summaryResId)
    }

    override fun getMetricsCategory(): Int = MetricsEvent.DERPFEST

    companion object {
        const val TAG = "DerpFestCustomizations"

        private const val KEY_QS_PANEL_STYLE = "qs_panel_style"
        private const val KEY_TILE_LABEL_HIDE = "qs_tile_label_hide"
        private const val KEY_QS_TILE_ICON_SHAPE = "qs_tile_icon_shape"
    }
}
