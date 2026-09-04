/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import com.android.internal.logging.nano.MetricsProto.MetricsEvent

import android.database.ContentObserver
import android.os.Bundle
import android.provider.Settings

import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import org.derpfest.support.preferences.ProperSeekBarPreference

import com.android.settings.R
import com.android.settings.SettingsPreferenceFragment
import com.android.settingslib.widget.BannerMessagePreference

class QsLayoutSettings : SettingsPreferenceFragment(), Preference.OnPreferenceChangeListener {

    private lateinit var mQsColumnsPreference: ProperSeekBarPreference
    private lateinit var mQsRowsPreference: ProperSeekBarPreference
    private lateinit var mQsColumnsLandscapePreference: ProperSeekBarPreference
    private lateinit var mQsRowsLandscapePreference: ProperSeekBarPreference
    private lateinit var mQqsRowsPreference: ProperSeekBarPreference
    private lateinit var mQqsRowsLandscapePreference: ProperSeekBarPreference
    private var mQsCategory: PreferenceCategory? = null
    private var mQqsCategory: PreferenceCategory? = null
    private var mDualShadeBanner: BannerMessagePreference? = null
    private var mDualShadeObserver: ContentObserver? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.qs_layout_settings)
        mQsCategory = findPreference(KEY_QS_CATEGORY)
        mQqsCategory = findPreference(KEY_QQS_CATEGORY)
        mDualShadeBanner = findPreference(KEY_DUAL_SHADE_BANNER)
        mDualShadeBanner?.let {
            QsShadePanels.bindBanner(
                it,
                requireContext(),
                metricsCategory,
                R.string.qs_layout_dual_shade_banner_title,
                R.string.qs_layout_dual_shade_banner_summary,
            )
        }
        
        mQsColumnsPreference = findPreference("qs_tiles_columns")!!
        mQsRowsPreference = findPreference("qs_tiles_rows")!!
        mQsColumnsLandscapePreference = findPreference("qs_tiles_columns_landscape")!!
        mQsRowsLandscapePreference = findPreference("qs_tiles_rows_landscape")!!
        mQqsRowsPreference = findPreference("qqs_tiles_rows")!!
        mQqsRowsLandscapePreference = findPreference("qqs_tiles_rows_landscape")!!
        
        mQsColumnsPreference.setOnPreferenceChangeListener(this)
        mQsRowsPreference.setOnPreferenceChangeListener(this)
        mQsColumnsLandscapePreference.setOnPreferenceChangeListener(this)
        mQsRowsLandscapePreference.setOnPreferenceChangeListener(this)
        mQqsRowsPreference.setOnPreferenceChangeListener(this)
        mQqsRowsLandscapePreference.setOnPreferenceChangeListener(this)
        
        // Set initial values from system settings
        setInitialValues()
        applyDualShadeAvailability()
    }

    override fun onStart() {
        super.onStart()
        mDualShadeObserver =
            QsShadePanels.registerDualShadeObserver(requireContext().contentResolver) {
                applyDualShadeAvailability()
            }
        applyDualShadeAvailability()
    }

    override fun onStop() {
        mDualShadeObserver?.let { requireContext().contentResolver.unregisterContentObserver(it) }
        mDualShadeObserver = null
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        // Refresh values when returning to the page
        setInitialValues()
        applyDualShadeAvailability()
    }

    private fun applyDualShadeAvailability() {
        val dualShade = QsShadePanels.isDualShadeEnabled(requireContext())
        mDualShadeBanner?.isVisible = dualShade
        mQsCategory?.isVisible = !dualShade
        mQqsCategory?.isVisible = !dualShade
    }

    private fun setInitialValues() {
        val cr = requireContext().contentResolver
        // Load current values from system settings (0 = use system default)
        val columnsValue = Settings.System.getInt(cr, Settings.System.QS_LAYOUT_COLUMNS, 0)
        val rowsValue = Settings.System.getInt(cr, Settings.System.QS_LAYOUT_ROWS, 0)
        val qqsRowsValue = Settings.System.getInt(cr, Settings.System.QQS_LAYOUT_ROWS, 0)
        val columnsLandscapeValue = Settings.System.getInt(cr, Settings.System.QS_LAYOUT_COLUMNS_LANDSCAPE, 0)
        val rowsLandscapeValue = Settings.System.getInt(cr, Settings.System.QS_LAYOUT_ROWS_LANDSCAPE, 0)
        val qqsRowsLandscapeValue = Settings.System.getInt(cr, Settings.System.QQS_LAYOUT_ROWS_LANDSCAPE, 0)

        // Set seekbar values (0 means use default; show as min for display)
        mQsColumnsPreference.setValue(if (columnsValue > 0) columnsValue else 4)
        mQsRowsPreference.setValue(if (rowsValue > 0) rowsValue else 4)
        mQsColumnsLandscapePreference.setValue(if (columnsLandscapeValue > 0) columnsLandscapeValue else 6)
        mQsRowsLandscapePreference.setValue(if (rowsLandscapeValue > 0) rowsLandscapeValue else 2)
        mQqsRowsPreference.setValue(if (qqsRowsValue > 0) qqsRowsValue else 2)
        mQqsRowsLandscapePreference.setValue(if (qqsRowsLandscapeValue > 0) qqsRowsLandscapeValue else 1)
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val cr = requireContext().contentResolver
        when (preference.key) {
            "qs_tiles_columns" -> {
                val seekbarValue = newValue as? Int ?: return false
                Settings.System.putInt(cr, Settings.System.QS_LAYOUT_COLUMNS, seekbarValue)
                mQsColumnsPreference.setValue(seekbarValue)
                return true
            }
            "qs_tiles_rows" -> {
                val seekbarValue = newValue as? Int ?: return false
                Settings.System.putInt(cr, Settings.System.QS_LAYOUT_ROWS, seekbarValue)
                mQsRowsPreference.setValue(seekbarValue)
                return true
            }
            "qqs_tiles_rows" -> {
                val seekbarValue = newValue as? Int ?: return false
                Settings.System.putInt(cr, Settings.System.QQS_LAYOUT_ROWS, seekbarValue)
                mQqsRowsPreference.setValue(seekbarValue)
                return true
            }
            "qs_tiles_columns_landscape" -> {
                val seekbarValue = newValue as? Int ?: return false
                Settings.System.putInt(cr, Settings.System.QS_LAYOUT_COLUMNS_LANDSCAPE, seekbarValue)
                mQsColumnsLandscapePreference.setValue(seekbarValue)
                return true
            }
            "qs_tiles_rows_landscape" -> {
                val seekbarValue = newValue as? Int ?: return false
                Settings.System.putInt(cr, Settings.System.QS_LAYOUT_ROWS_LANDSCAPE, seekbarValue)
                mQsRowsLandscapePreference.setValue(seekbarValue)
                return true
            }
            "qqs_tiles_rows_landscape" -> {
                val seekbarValue = newValue as? Int ?: return false
                Settings.System.putInt(cr, Settings.System.QQS_LAYOUT_ROWS_LANDSCAPE, seekbarValue)
                mQqsRowsLandscapePreference.setValue(seekbarValue)
                return true
            }
        }
        return true
    }

    override fun getMetricsCategory(): Int = MetricsEvent.DERPFEST

    companion object {
        const val TAG = "QsLayoutSettings"

        private const val KEY_QS_CATEGORY = "qs_layout_category"
        private const val KEY_QQS_CATEGORY = "qqs_layout_category"
        private const val KEY_DUAL_SHADE_BANNER = "qs_layout_dual_shade_banner"
    }
}
