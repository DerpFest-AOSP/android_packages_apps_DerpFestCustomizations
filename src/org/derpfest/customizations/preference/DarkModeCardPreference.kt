/*
 * SPDX-FileCopyrightText: Project Fluid
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.preference;

import android.app.UiModeManager
import android.content.Context
import android.util.AttributeSet

import com.android.settings.R

import org.derpfest.ui.preference.DerpFestCardDarkModePreferenceBase

class DarkModeCardPreference(context: Context, attrs: AttributeSet) : DerpFestCardDarkModePreferenceBase(context, attrs) {

    private val uiModeManager: UiModeManager = context.getSystemService(UiModeManager::class.java)

    override fun setNightModeActivated(checked: Boolean) {
        uiModeManager.nightMode = when (checked) {
            true -> UiModeManager.MODE_NIGHT_YES
            false -> UiModeManager.MODE_NIGHT_NO
        }
    }

    override val defaultSummary = context.resources.getString(R.string.dark_mode_summary_default)
    override val powerSaveSummary = context.resources.getString(R.string.dark_mode_summary_powersave)
}
