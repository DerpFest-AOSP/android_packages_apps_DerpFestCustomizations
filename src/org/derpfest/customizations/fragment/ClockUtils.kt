/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */
package org.derpfest.customizations.fragment

import android.content.Context
import com.android.internal.util.derpfest.ThemeUtils
import com.android.settings.R

object ClockUtils {

    /** Must stay in sync with {@code R.array.lockscreen_clock_names}. */
    @JvmField
    val CLOCK_LAYOUTS = intArrayOf(
        R.layout.keyguard_clock_default,
        R.layout.keyguard_clock_oos, // 1
        R.layout.keyguard_clock_ios, // 2
        R.layout.keyguard_clock_simple, // 3
        R.layout.keyguard_clock_miui, // 4
        R.layout.keyguard_clock_ide, // 5
        R.layout.keyguard_clock_moto, // 6
        R.layout.keyguard_clock_stylish, // 7
        R.layout.keyguard_clock_stylish2, // 8
        R.layout.keyguard_clock_stylish3, // 9
        R.layout.keyguard_clock_stylish4, // 10
        R.layout.keyguard_clock_stylish5, // 11
        R.layout.keyguard_clock_stylish6, // 12
        R.layout.keyguard_clock_stylish7, // 13
        R.layout.keyguard_clock_stylish8, // 14
        R.layout.keyguard_clock_stylish9, // 15
        R.layout.keyguard_clock_stylish10, // 16
        R.layout.keyguard_clock_word, // 17
        R.layout.keyguard_clock_life, // 18
        R.layout.keyguard_clock_a9, // 19
        R.layout.keyguard_clock_nos1, // 20
        R.layout.keyguard_clock_nos2, // 21
        R.layout.keyguard_clock_num, // 22
        R.layout.keyguard_clock_accent, // 23
        R.layout.keyguard_clock_analog, // 24
        R.layout.keyguard_clock_block, // 25
        R.layout.keyguard_clock_bubble, // 26
        R.layout.keyguard_clock_label, // 27
        R.layout.keyguard_clock_taden, // 28
        R.layout.keyguard_clock_mont, // 29
        R.layout.keyguard_clock_encode, // 30
        R.layout.keyguard_clock_nos3, // 31
    )

    @JvmStatic
    fun getClockNames(context: Context): Array<String> {
        return context.resources.getStringArray(R.array.lockscreen_clock_names)
    }

    @JvmStatic
    fun updateClockOverlays(themeUtils: ThemeUtils, clockStyle: Int) {
        themeUtils.setOverlayEnabled(
            "android.theme.customization.hideclock",
            if (clockStyle != 0) "com.android.systemui.clocks.hideclock" else "android",
            "android",
        )
        themeUtils.setOverlayEnabled(
            "android.theme.customization.smartspace",
            if (clockStyle != 0) "com.android.systemui.hide.smartspace" else "com.android.systemui",
            "com.android.systemui",
        )
    }
}
