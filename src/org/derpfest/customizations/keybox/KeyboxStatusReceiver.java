/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.keybox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Arms the periodic keybox check and runs one pass after boot. */
public class KeyboxStatusReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        KeyboxStatusJobService.ensureScheduled(context);
        KeyboxStatusJobService.enqueueSoon(context);
    }
}
