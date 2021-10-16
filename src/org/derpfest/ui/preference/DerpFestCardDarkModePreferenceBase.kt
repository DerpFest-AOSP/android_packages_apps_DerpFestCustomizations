/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.ui.preference

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.util.AttributeSet

abstract class DerpFestCardDarkModePreferenceBase(
    context: Context,
    attrs: AttributeSet
) : DerpFestCardSwitchPreference(context, attrs) {

    private val powerManager: PowerManager = context.getSystemService(PowerManager::class.java)
    
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateSummaryAndState()
        }
    }
    
    private fun isDarkModeEnabled(): Boolean {
        val uiMode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return when (uiMode) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            android.content.res.Configuration.UI_MODE_NIGHT_NO -> false
            else -> false
        }
    }
    
    private fun updateSummaryAndState() {
        val isPowerSaveMode = powerManager.isPowerSaveMode
        setEnabled(!isPowerSaveMode)
        
        val summaryText = when {
            isPowerSaveMode -> powerSaveSummary
            else -> defaultSummary
        }
        summary = summaryText
    }
    
    override fun onClick() {
        super.onClick()
        setNightModeActivated(!isDarkModeEnabled())
    }
    
    override fun onAttached() {
        super.onAttached()
        context.registerReceiver(receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
        updateSummaryAndState()
        isChecked = isDarkModeEnabled()
    }
    
    override fun onDetached() {
        context.unregisterReceiver(receiver)
    }
    
    abstract fun setNightModeActivated(checked: Boolean)
    
    abstract val defaultSummary: String
    abstract val powerSaveSummary: String
} 
