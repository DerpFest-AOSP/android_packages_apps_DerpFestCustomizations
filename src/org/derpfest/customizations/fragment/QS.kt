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
import androidx.preference.PreferenceCategory

import org.derpfest.support.preferences.ProperSeekBarPreference

import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment

class QS : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    private lateinit var mDataUsagePreference: Preference
    private lateinit var mDataUsageCycleTypePreference: ListPreference
    private var mTileLabelHide: Preference? = null
    private var mTileIconShape: Preference? = null
    private var mClassicLayoutCategory: PreferenceCategory? = null
    private lateinit var mQsColumnsClassic: ProperSeekBarPreference
    private lateinit var mQsColumnsLandscapeClassic: ProperSeekBarPreference
    private lateinit var mQqsColumnsClassic: ProperSeekBarPreference
    private lateinit var mQqsColumnsLandscapeClassic: ProperSeekBarPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.qs)

        findPreference<Preference>(KEY_QS_PANEL_STYLE)?.setOnPreferenceChangeListener(this)
        mTileLabelHide = findPreference(KEY_TILE_LABEL_HIDE)
        mTileIconShape = findPreference(KEY_QS_TILE_ICON_SHAPE)
        mClassicLayoutCategory = findPreference(KEY_CLASSIC_LAYOUT_CATEGORY)
        mQsColumnsClassic = findPreference(KEY_QS_TILES_COLUMNS_CLASSIC)!!
        mQsColumnsLandscapeClassic = findPreference(KEY_QS_TILES_COLUMNS_LANDSCAPE_CLASSIC)!!
        mQqsColumnsClassic = findPreference(KEY_QQS_TILES_COLUMNS_CLASSIC)!!
        mQqsColumnsLandscapeClassic = findPreference(KEY_QQS_TILES_COLUMNS_LANDSCAPE_CLASSIC)!!

        listOf(
            mQsColumnsClassic,
            mQsColumnsLandscapeClassic,
            mQqsColumnsClassic,
            mQqsColumnsLandscapeClassic,
        ).forEach { it.setOnPreferenceChangeListener(this) }

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
        val cr = requireContext().contentResolver
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
            KEY_QS_TILES_COLUMNS_CLASSIC -> {
                val v = newValue as? Int ?: return false
                Settings.System.putInt(cr, SYSTEM_QS_LAYOUT_COLUMNS_CLASSIC, v)
                mQsColumnsClassic.setValue(v)
                return true
            }
            KEY_QS_TILES_COLUMNS_LANDSCAPE_CLASSIC -> {
                val v = newValue as? Int ?: return false
                Settings.System.putInt(cr, SYSTEM_QS_LAYOUT_COLUMNS_LANDSCAPE_CLASSIC, v)
                mQsColumnsLandscapeClassic.setValue(v)
                return true
            }
            KEY_QQS_TILES_COLUMNS_CLASSIC -> {
                val v = newValue as? Int ?: return false
                Settings.System.putInt(cr, SYSTEM_QQS_LAYOUT_COLUMNS_CLASSIC, v)
                mQqsColumnsClassic.setValue(v)
                return true
            }
            KEY_QQS_TILES_COLUMNS_LANDSCAPE_CLASSIC -> {
                val v = newValue as? Int ?: return false
                Settings.System.putInt(cr, SYSTEM_QQS_LAYOUT_COLUMNS_LANDSCAPE_CLASSIC, v)
                mQqsColumnsLandscapeClassic.setValue(v)
                return true
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        if (Settings.Secure.getIntForUser(
                requireContext().contentResolver,
                KEY_QS_PANEL_STYLE,
                0,
                UserHandle.USER_CURRENT,
            ) == 1
        ) {
            setInitialClassicLayoutValues()
        }
    }

    private fun updateCircularPrefs(circular: Boolean) {
        mTileLabelHide?.isVisible = circular
        mTileIconShape?.isVisible = circular
        mClassicLayoutCategory?.isVisible = circular
        if (circular) {
            setInitialClassicLayoutValues()
        }
    }

    private fun setInitialClassicLayoutValues() {
        val cr = requireContext().contentResolver
        val qsPort =
            Settings.System.getInt(cr, SYSTEM_QS_LAYOUT_COLUMNS_CLASSIC, 0).let {
                if (it > 0) it else DEFAULT_QS_CLASSIC_COLUMNS_PORT
            }
        val qsLand =
            Settings.System.getInt(cr, SYSTEM_QS_LAYOUT_COLUMNS_LANDSCAPE_CLASSIC, 0).let {
                if (it > 0) it else DEFAULT_QS_CLASSIC_COLUMNS_LAND
            }
        val qqsPort =
            Settings.System.getInt(cr, SYSTEM_QQS_LAYOUT_COLUMNS_CLASSIC, 0).let {
                if (it > 0) it else DEFAULT_QQS_CLASSIC_COLUMNS_PORT
            }
        val qqsLand =
            Settings.System.getInt(cr, SYSTEM_QQS_LAYOUT_COLUMNS_LANDSCAPE_CLASSIC, 0).let {
                if (it > 0) it else DEFAULT_QQS_CLASSIC_COLUMNS_LAND
            }
        mQsColumnsClassic.setValue(qsPort)
        mQsColumnsLandscapeClassic.setValue(qsLand)
        mQqsColumnsClassic.setValue(qqsPort)
        mQqsColumnsLandscapeClassic.setValue(qqsLand)
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
        private const val KEY_CLASSIC_LAYOUT_CATEGORY = "qs_classic_layout_category"
        private const val KEY_QS_TILES_COLUMNS_CLASSIC = "qs_tiles_columns_classic"
        private const val KEY_QS_TILES_COLUMNS_LANDSCAPE_CLASSIC =
            "qs_tiles_columns_landscape_classic"
        private const val KEY_QQS_TILES_COLUMNS_CLASSIC = "qqs_tiles_columns_classic"
        private const val KEY_QQS_TILES_COLUMNS_LANDSCAPE_CLASSIC =
            "qqs_tiles_columns_landscape_classic"

        /** Matches [android.provider.Settings.System] keys consumed by SystemUI. */
        private const val SYSTEM_QS_LAYOUT_COLUMNS_CLASSIC = "qs_layout_columns_classic"
        private const val SYSTEM_QS_LAYOUT_COLUMNS_LANDSCAPE_CLASSIC =
            "qs_layout_columns_landscape_classic"
        private const val SYSTEM_QQS_LAYOUT_COLUMNS_CLASSIC = "qqs_layout_columns_classic"
        private const val SYSTEM_QQS_LAYOUT_COLUMNS_LANDSCAPE_CLASSIC =
            "qqs_layout_columns_landscape_classic"

        private const val DEFAULT_QS_CLASSIC_COLUMNS_PORT = 4
        private const val DEFAULT_QS_CLASSIC_COLUMNS_LAND = 6
        private const val DEFAULT_QQS_CLASSIC_COLUMNS_PORT = 4
        private const val DEFAULT_QQS_CLASSIC_COLUMNS_LAND = 6
    }
}
