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
import com.android.settings.system.ShadePanelsPreferenceController
import com.android.settingslib.widget.BannerMessagePreference
import com.android.systemui.Flags

/** Helpers for QS layout vs. Separate panels (dual shade). */
object QsShadePanels {

    /** Matches [ShadePanelsPreferenceController] and SystemUI when no user preference is set. */
    const val DUAL_SHADE_DEFAULT = 1

    /** Same flag gate as [ShadePanelsPreferenceController]. */
    fun isDualShadeFeatureEnabled(): Boolean = Flags.sceneContainer() && Flags.dualShade()

    /** Whether the user can open Notifications & Quick Settings to change the mode. */
    fun canChangeShadePanels(context: Context): Boolean =
        ShadePanelsPreferenceController.isDualShadeAvailable(context)

    /**
     * Whether SystemUI is actually using Separate panels.
     *
     * Flags off → never, even if [Settings.Secure.DUAL_SHADE] is still 1. Setting page available →
     * the user preference (default on). Setting page hidden (e.g. tablets) → SystemUI ignores the
     * secure setting and uses `config_dualShadeEnabledByDefault` (true).
     */
    fun isDualShadeEnabled(context: Context): Boolean {
        if (!isDualShadeFeatureEnabled()) {
            return false
        }
        if (canChangeShadePanels(context)) {
            return Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.DUAL_SHADE,
                DUAL_SHADE_DEFAULT,
            ) == DUAL_SHADE_DEFAULT
        }
        return true
    }

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
        if (canChangeShadePanels(context)) {
            banner.setPositiveButtonVisible(true)
            banner.setPositiveButtonText(R.string.qs_layout_open_shade_panels)
            banner.setPositiveButtonOnClickListener { launchShadePanels(context, metricsCategory) }
        } else {
            banner.setPositiveButtonVisible(false)
            banner.setPositiveButtonOnClickListener(null)
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
