/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import com.android.internal.logging.nano.MetricsProto.MetricsEvent

import android.os.Bundle
import android.provider.Settings

import androidx.preference.Preference
import org.derpfest.support.preferences.ProperSeekBarPreference

import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment

/** Rows/columns for circular (classic) QS; mirrors [QsLayoutSettings] for card/infinite grid. */
class QsClassicLayoutSettings : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    private lateinit var mQsColumnsClassic: ProperSeekBarPreference
    private lateinit var mQsColumnsLandscapeClassic: ProperSeekBarPreference
    private lateinit var mQqsColumnsClassic: ProperSeekBarPreference
    private lateinit var mQqsColumnsLandscapeClassic: ProperSeekBarPreference
    private lateinit var mQsRowsClassic: ProperSeekBarPreference
    private lateinit var mQsRowsLandscapeClassic: ProperSeekBarPreference
    private lateinit var mQqsRowsClassic: ProperSeekBarPreference
    private lateinit var mQqsRowsLandscapeClassic: ProperSeekBarPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.qs_classic_layout_settings)

        mQsColumnsClassic = findPreference(KEY_QS_TILES_COLUMNS_CLASSIC)!!
        mQsColumnsLandscapeClassic = findPreference(KEY_QS_TILES_COLUMNS_LANDSCAPE_CLASSIC)!!
        mQqsColumnsClassic = findPreference(KEY_QQS_TILES_COLUMNS_CLASSIC)!!
        mQqsColumnsLandscapeClassic = findPreference(KEY_QQS_TILES_COLUMNS_LANDSCAPE_CLASSIC)!!
        mQsRowsClassic = findPreference(KEY_QS_TILES_ROWS_CLASSIC)!!
        mQsRowsLandscapeClassic = findPreference(KEY_QS_TILES_ROWS_LANDSCAPE_CLASSIC)!!
        mQqsRowsClassic = findPreference(KEY_QQS_TILES_ROWS_CLASSIC)!!
        mQqsRowsLandscapeClassic = findPreference(KEY_QQS_TILES_ROWS_LANDSCAPE_CLASSIC)!!

        listOf(
            mQsColumnsClassic,
            mQsColumnsLandscapeClassic,
            mQqsColumnsClassic,
            mQqsColumnsLandscapeClassic,
            mQsRowsClassic,
            mQsRowsLandscapeClassic,
            mQqsRowsClassic,
            mQqsRowsLandscapeClassic,
        ).forEach { it.setOnPreferenceChangeListener(this) }

        setInitialValues()
    }

    override fun onResume() {
        super.onResume()
        setInitialValues()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val cr = requireContext().contentResolver
        val v = newValue as? Int ?: return false
        when (preference.key) {
            KEY_QS_TILES_COLUMNS_CLASSIC -> {
                Settings.System.putInt(cr, SYSTEM_QS_LAYOUT_COLUMNS_CLASSIC, v)
                mQsColumnsClassic.setValue(v)
            }
            KEY_QS_TILES_COLUMNS_LANDSCAPE_CLASSIC -> {
                Settings.System.putInt(cr, SYSTEM_QS_LAYOUT_COLUMNS_LANDSCAPE_CLASSIC, v)
                mQsColumnsLandscapeClassic.setValue(v)
            }
            KEY_QQS_TILES_COLUMNS_CLASSIC -> {
                Settings.System.putInt(cr, SYSTEM_QQS_LAYOUT_COLUMNS_CLASSIC, v)
                mQqsColumnsClassic.setValue(v)
            }
            KEY_QQS_TILES_COLUMNS_LANDSCAPE_CLASSIC -> {
                Settings.System.putInt(cr, SYSTEM_QQS_LAYOUT_COLUMNS_LANDSCAPE_CLASSIC, v)
                mQqsColumnsLandscapeClassic.setValue(v)
            }
            KEY_QS_TILES_ROWS_CLASSIC -> {
                Settings.System.putInt(cr, SYSTEM_QS_LAYOUT_ROWS_CLASSIC, v)
                mQsRowsClassic.setValue(v)
            }
            KEY_QS_TILES_ROWS_LANDSCAPE_CLASSIC -> {
                Settings.System.putInt(cr, SYSTEM_QS_LAYOUT_ROWS_LANDSCAPE_CLASSIC, v)
                mQsRowsLandscapeClassic.setValue(v)
            }
            KEY_QQS_TILES_ROWS_CLASSIC -> {
                Settings.System.putInt(cr, SYSTEM_QQS_LAYOUT_ROWS_CLASSIC, v)
                mQqsRowsClassic.setValue(v)
            }
            KEY_QQS_TILES_ROWS_LANDSCAPE_CLASSIC -> {
                Settings.System.putInt(cr, SYSTEM_QQS_LAYOUT_ROWS_LANDSCAPE_CLASSIC, v)
                mQqsRowsLandscapeClassic.setValue(v)
            }
            else -> return false
        }
        return true
    }

    private fun setInitialValues() {
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
        val qsRowsPort =
            Settings.System.getInt(cr, SYSTEM_QS_LAYOUT_ROWS_CLASSIC, 0).let {
                if (it > 0) it else DEFAULT_QS_CLASSIC_ROWS_PORT
            }
        val qsRowsLand =
            Settings.System.getInt(cr, SYSTEM_QS_LAYOUT_ROWS_LANDSCAPE_CLASSIC, 0).let {
                if (it > 0) it else DEFAULT_QS_CLASSIC_ROWS_LAND
            }
        val qqsRowsPort =
            Settings.System.getInt(cr, SYSTEM_QQS_LAYOUT_ROWS_CLASSIC, 0).let {
                if (it > 0) it else DEFAULT_QQS_CLASSIC_ROWS_PORT
            }
        val qqsRowsLand =
            Settings.System.getInt(cr, SYSTEM_QS_LAYOUT_ROWS_LANDSCAPE_CLASSIC, 0).let {
                if (it > 0) it else DEFAULT_QQS_CLASSIC_ROWS_LAND
            }
        mQsColumnsClassic.setValue(qsPort)
        mQsColumnsLandscapeClassic.setValue(qsLand)
        mQqsColumnsClassic.setValue(qqsPort)
        mQqsColumnsLandscapeClassic.setValue(qqsLand)
        mQsRowsClassic.setValue(qsRowsPort)
        mQsRowsLandscapeClassic.setValue(qsRowsLand)
        mQqsRowsClassic.setValue(qqsRowsPort)
        mQqsRowsLandscapeClassic.setValue(qqsRowsLand)
    }

    override fun getMetricsCategory(): Int = MetricsEvent.DERPFEST

    companion object {
        const val TAG = "QsClassicLayoutSettings"

        private const val KEY_QS_TILES_COLUMNS_CLASSIC = "qs_tiles_columns_classic"
        private const val KEY_QS_TILES_COLUMNS_LANDSCAPE_CLASSIC =
            "qs_tiles_columns_landscape_classic"
        private const val KEY_QQS_TILES_COLUMNS_CLASSIC = "qqs_tiles_columns_classic"
        private const val KEY_QQS_TILES_COLUMNS_LANDSCAPE_CLASSIC =
            "qqs_tiles_columns_landscape_classic"
        private const val KEY_QS_TILES_ROWS_CLASSIC = "qs_tiles_rows_classic"
        private const val KEY_QS_TILES_ROWS_LANDSCAPE_CLASSIC =
            "qs_tiles_rows_landscape_classic"
        private const val KEY_QQS_TILES_ROWS_CLASSIC = "qqs_tiles_rows_classic"
        private const val KEY_QQS_TILES_ROWS_LANDSCAPE_CLASSIC =
            "qqs_tiles_rows_landscape_classic"

        private const val SYSTEM_QS_LAYOUT_COLUMNS_CLASSIC = "qs_layout_columns_classic"
        private const val SYSTEM_QS_LAYOUT_COLUMNS_LANDSCAPE_CLASSIC =
            "qs_layout_columns_landscape_classic"
        private const val SYSTEM_QQS_LAYOUT_COLUMNS_CLASSIC = "qqs_layout_columns_classic"
        private const val SYSTEM_QQS_LAYOUT_COLUMNS_LANDSCAPE_CLASSIC =
            "qqs_layout_columns_landscape_classic"
        private const val SYSTEM_QS_LAYOUT_ROWS_CLASSIC = "qs_layout_rows_classic"
        private const val SYSTEM_QS_LAYOUT_ROWS_LANDSCAPE_CLASSIC =
            "qs_layout_rows_landscape_classic"
        private const val SYSTEM_QQS_LAYOUT_ROWS_CLASSIC = "qqs_layout_rows_classic"
        private const val SYSTEM_QQS_LAYOUT_ROWS_LANDSCAPE_CLASSIC =
            "qqs_layout_rows_landscape_classic"

        private const val DEFAULT_QS_CLASSIC_COLUMNS_PORT = 4
        private const val DEFAULT_QS_CLASSIC_COLUMNS_LAND = 6
        private const val DEFAULT_QQS_CLASSIC_COLUMNS_PORT = 4
        private const val DEFAULT_QQS_CLASSIC_COLUMNS_LAND = 6
        private const val DEFAULT_QS_CLASSIC_ROWS_PORT = 3
        private const val DEFAULT_QS_CLASSIC_ROWS_LAND = 2
        private const val DEFAULT_QQS_CLASSIC_ROWS_PORT = 2
        private const val DEFAULT_QQS_CLASSIC_ROWS_LAND = 1
    }
}
