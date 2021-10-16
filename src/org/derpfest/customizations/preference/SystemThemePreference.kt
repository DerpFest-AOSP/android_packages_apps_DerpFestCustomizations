/*
 * SPDX-FileCopyrightText: Project Fluid
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.Toast

import org.derpfest.ui.preference.DerpFestSystemThemePreferenceBase

class SystemThemePreference(context: Context, attributeSet: AttributeSet)
    : DerpFestSystemThemePreferenceBase(context, attributeSet) {

    // open the theme settings when button is pressed
    override fun openThemeSettings() {
        Toast.makeText(context, "Monet settings coming soon", Toast.LENGTH_SHORT).show()
    }

    // check if Monet is enabled
    override fun isMonetEnabled(): Boolean {
        // For now, we'll just return true. In a full implementation, 
        // this should check if Monet is enabled in the system
        return true
    }

    // toggle Monet on or off system-wide
    override fun toggleMonet(activated: Boolean?) {
        // For now, we'll just show a toast. In a full implementation,
        // this should toggle Monet in the system
        activated?.let {
            val message = if (it) "Monet enabled" else "Monet disabled"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
