/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.android.settings.R
import com.android.settings.core.SubSettingLauncher
import com.android.settings.system.ShadePanelsFragment
import com.android.settingslib.widget.BannerMessagePreference

/** Helpers for QS layout vs. [Settings.Secure.DUAL_SHADE] (Separate panels). */
object QsShadePanels {

    /** Matches [com.android.settings.system.ShadePanelsPreferenceController] default. */
    const val DUAL_SHADE_DEFAULT = 1

    fun isDualShadeEnabled(resolver: ContentResolver): Boolean =
        Settings.Secure.getInt(resolver, Settings.Secure.DUAL_SHADE, DUAL_SHADE_DEFAULT) == 1

    fun isDualShadeEnabled(context: Context): Boolean = isDualShadeEnabled(context.contentResolver)

    fun launchShadePanels(context: Context, metricsCategory: Int) {
        SubSettingLauncher(context)
            .setDestination(ShadePanelsFragment::class.java.name)
            .setSourceMetricsCategory(metricsCategory)
            .setTitleRes(R.string.shade_panels_title)
            .launch()
    }

    fun bindBanner(
        banner: BannerMessagePreference,
        context: Context,
        metricsCategory: Int,
        titleRes: Int,
        summaryRes: Int,
    ) {
        banner.setTitle(titleRes)
        banner.setSummary(summaryRes)
        banner.setAttentionLevel(BannerMessagePreference.AttentionLevel.LOW)
        banner.setPositiveButtonText(R.string.qs_layout_open_shade_panels)
        banner.setPositiveButtonOnClickListener {
            launchShadePanels(context, metricsCategory)
        }
    }

    fun registerDualShadeObserver(resolver: ContentResolver, onChange: () -> Unit): ContentObserver {
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    onChange()
                }
            }
        resolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.DUAL_SHADE),
            /* notifyForDescendants */ false,
            observer,
        )
        return observer
    }
}
