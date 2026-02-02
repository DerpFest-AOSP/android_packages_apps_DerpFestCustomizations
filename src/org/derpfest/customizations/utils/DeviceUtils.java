/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */
package org.derpfest.customizations.utils;

import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Vibrator;

import androidx.annotation.NonNull;

public class DeviceUtils {

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
