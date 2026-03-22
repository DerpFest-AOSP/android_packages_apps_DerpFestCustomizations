/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.utils;

import android.content.Context;

import com.android.internal.util.derpfest.SystemUiRestart;

/**
 * Helpers for interacting with SystemUI from Settings (privileged).
 */
public final class SystemUiUtils {

    private SystemUiUtils() {}

    /**
     * Restarts SystemUI so lock screen theme/overlay changes take effect immediately.
     * Uses {@link IStatusBarService#restartSystemUI()} (SystemUI kills its own process);
     * {@link android.app.ActivityManager#forceStopPackage} does not stop persistent apps.
     */
    public static void restartSystemUI(Context context) {
        if (context == null) {
            return;
        }
        SystemUiRestart.restartSystemUI();
    }
}
