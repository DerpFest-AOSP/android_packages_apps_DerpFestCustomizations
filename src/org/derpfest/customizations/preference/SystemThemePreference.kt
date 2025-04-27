/*
 * SPDX-FileCopyrightText: Project Fluid
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.preference

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.widget.Toast
import androidx.fragment.app.FragmentActivity

import org.derpfest.ui.preference.DerpFestSystemThemePreferenceBase
import org.derpfest.customizations.fragment.MonetSettings

class SystemThemePreference(context: Context, attributeSet: AttributeSet)
    : DerpFestSystemThemePreferenceBase(context, attributeSet) {

    // open the theme settings when button is pressed
    override fun openThemeSettings() {
        // Launch MonetSettings fragment using standard Settings approach
        val intent = Intent(context, com.android.settings.SubSettings::class.java)
        intent.putExtra(":settings:show_fragment", MonetSettings::class.java.name)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        val title = context.getString(com.android.settings.R.string.monet_settings)
        intent.putExtra(":settings:show_fragment_title_resid",
                        com.android.settings.R.string.monet_settings)
        
        context.startActivity(intent)
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
