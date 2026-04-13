/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations

import android.content.Context
import android.content.SharedPreferences

/** Persists unlock flags for hidden preferences (e.g. About easter egg). */
object DerpfestEasterEggPrefs {
    private const val PREFS_NAME = "derpfest_customizations_easter_eggs"
    private const val KEY_ABOUT_LOGO_EASTER_EGG_FINISHED = "about_derpfest_logo_easter_egg_finished"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** After the About logo easter egg dialog is completed (Got it). */
    fun isAboutLogoEasterEggFinished(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ABOUT_LOGO_EASTER_EGG_FINISHED, false)

    fun setAboutLogoEasterEggFinished(context: Context) {
        prefs(context).edit().putBoolean(KEY_ABOUT_LOGO_EASTER_EGG_FINISHED, true).apply()
    }
}
