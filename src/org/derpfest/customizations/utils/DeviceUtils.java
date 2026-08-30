/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */
package org.derpfest.customizations.utils;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Vibrator;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.Surface;

import androidx.annotation.NonNull;

public class DeviceUtils {

    /** Returns whether the device has a centered display cutout. */
    public static boolean hasCenteredCutout(Context context) {
        Display display = context.getDisplay();
        if (display == null) {
            return false;
        }
        DisplayCutout cutout = display.getCutout();
        if (cutout == null) {
            return false;
        }
        Point realSize = new Point();
        display.getRealSize(realSize);
        switch (display.getRotation()) {
            case Surface.ROTATION_0: {
                Rect rect = cutout.getBoundingRectTop();
                return !(rect.left <= 0 || rect.right >= realSize.x);
            }
            case Surface.ROTATION_90: {
                Rect rect = cutout.getBoundingRectLeft();
                return !(rect.top <= 0 || rect.bottom >= realSize.y);
            }
            case Surface.ROTATION_180: {
                Rect rect = cutout.getBoundingRectBottom();
                return !(rect.left <= 0 || rect.right >= realSize.x);
            }
            case Surface.ROTATION_270: {
                Rect rect = cutout.getBoundingRectRight();
                return !(rect.top <= 0 || rect.bottom >= realSize.y);
            }
            default:
                return false;
        }
    }

    public static boolean hasVibrator(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            return false;
        }
        return true;
    }

    public static boolean hasFingerprint(Context context) {
        FingerprintManager fp = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
        if (fp == null || !fp.isHardwareDetected()) {
            return false;
        }
        return true;
    }
}
