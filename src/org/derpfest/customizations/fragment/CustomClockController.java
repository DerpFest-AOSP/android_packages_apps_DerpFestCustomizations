/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class CustomClockController extends BasePreferenceController {

    public CustomClockController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        int index = Settings.Secure.getIntForUser(
                mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_STYLE,
                0, UserHandle.USER_CURRENT);
        String[] names = mContext.getResources().getStringArray(R.array.lockscreen_clock_names);
        if (index >= 0 && index < names.length) {
            return names[index];
        }
        return names.length > 0 ? names[0] : "";
    }
}
