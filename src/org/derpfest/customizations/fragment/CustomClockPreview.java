/*
 * SPDX-FileCopyrightText: 2023-2024 The risingOS Android Project
 * SPDX-FileCopyrightText: 2024-25 Project Infinity X
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */
package org.derpfest.customizations.fragment;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.derpfest.ThemeUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.widget.LayoutPreference;

import org.derpfest.customizations.utils.SystemUiUtils;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class CustomClockPreview extends SettingsPreferenceFragment {

    private static final String TAG = "CustomClockPreview";
    private static final String PREF_FIRST_TIME = "first_time_clock_face_access";

    private static final String KEY_CLOCK_PREVIEW = "clock_preview";

    private ViewPager viewPager;
    private ClockPagerAdapter pagerAdapter;
    private ExtendedFloatingActionButton applyFab;
    private View highlightGuide;
    private TextView clockNameTextView;

    private int mClockPosition = 0;

    private ThemeUtils mThemeUtils;

    private static final int[] CLOCK_LAYOUTS = {
            R.layout.keyguard_clock_default,
            R.layout.keyguard_clock_oos, // 1
            R.layout.keyguard_clock_ios, // 2
            R.layout.keyguard_clock_simple, // 3
            R.layout.keyguard_clock_miui, // 4
            R.layout.keyguard_clock_ide,  // 5
            R.layout.keyguard_clock_moto, // 6
            R.layout.keyguard_clock_stylish, // 7
            R.layout.keyguard_clock_stylish2, //8
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
            R.layout.keyguard_clock_nos3 // 31
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActivity().setTitle(getString(R.string.lockscreen_custom_clock_style_title));
        mThemeUtils = new ThemeUtils(getActivity());

        addPreferencesFromResource(R.xml.lockscreen_clock_preview_settings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView list = getListView();
        if (list != null) {
            list.setClipToPadding(false);
            list.setPadding(0, 0, 0, 0);
        }
        setupClockPreview();
    }

    private void setupClockPreview() {
        LayoutPreference clockPreviewPref = findPreference(KEY_CLOCK_PREVIEW);
        if (clockPreviewPref == null) return;

        clockNameTextView = clockPreviewPref.findViewById(R.id.clock_name);
        viewPager = clockPreviewPref.findViewById(R.id.view_pager);
        applyFab = clockPreviewPref.findViewById(R.id.apply_extended_fab);
        highlightGuide = clockPreviewPref.findViewById(R.id.highlight_guide);

        pagerAdapter = new ClockPagerAdapter();
        viewPager.setAdapter(pagerAdapter);
        viewPager.setClipChildren(true);
        viewPager.setClipToPadding(true);

        mClockPosition = Settings.Secure.getIntForUser(
                getContext().getContentResolver(), Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_STYLE, 0, UserHandle.USER_CURRENT);
        if (mClockPosition < 0 || mClockPosition >= CLOCK_LAYOUTS.length) {
            mClockPosition = 0;
            Settings.Secure.putIntForUser(
                    getContext().getContentResolver(), Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_STYLE, 0, UserHandle.USER_CURRENT);
        }
        viewPager.setCurrentItem(mClockPosition);

        applyFab.setOnClickListener(v -> {
            Context ctx = getContext();
            if (ctx == null || !isAdded()) {
                return;
            }
            int currentClock = Settings.Secure.getIntForUser(
                    ctx.getContentResolver(), Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_STYLE, 0, UserHandle.USER_CURRENT);
            if (mClockPosition == currentClock) {
                return;
            }
            new AlertDialog.Builder(ctx)
                    .setTitle(R.string.lockscreen_clock_restart_dialog_title)
                    .setMessage(R.string.lockscreen_clock_restart_dialog_message)
                    .setPositiveButton(R.string.lockscreen_clock_restart_apply,
                            (dialog, which) -> applyClockSelectionAndRestart())
                    .setNegativeButton(com.android.settings.R.string.cancel, null)
                    .show();
        });

        if (isFirstTime()) {
            highlightGuide.setVisibility(View.VISIBLE);
            highlightGuide.setOnClickListener(v -> {
                highlightGuide.setVisibility(View.GONE);
                disableHighlight();
            });
        } else {
            highlightGuide.setVisibility(View.GONE);
        }

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int state) {}
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
            @Override
            public void onPageSelected(int position) {
                mClockPosition = position;
                if (viewPager != null) {
                    viewPager.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                }
                updateClockName(position);
            }
        });

        updateClockName(mClockPosition);
    }

    /**
     * Persists clock style, updates overlays when default/custom boundary is crossed, and restarts
     * System UI. Call only after user confirms in the restart dialog.
     */
    private void applyClockSelectionAndRestart() {
        Context ctx = getContext();
        if (ctx == null) {
            return;
        }
        int currentClock = Settings.Secure.getIntForUser(
                ctx.getContentResolver(), Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_STYLE, 0, UserHandle.USER_CURRENT);
        if (mClockPosition == currentClock) {
            return;
        }
        // Overlays only depend on "default vs custom" (style == 0 or not). Updating
        // ThemeUtils / THEME_CUSTOMIZATION_OVERLAY_PACKAGES on every style change is redundant
        // and can trigger a full theme refresh (including lock wallpaper) unnecessarily.
        boolean wasCustom = currentClock != 0;
        boolean nowCustom = mClockPosition != 0;
        Settings.Secure.putIntForUser(
                ctx.getContentResolver(), Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_STYLE, mClockPosition, UserHandle.USER_CURRENT);
        if (wasCustom != nowCustom) {
            updateClockOverlays(mClockPosition);
        }
        SystemUiUtils.restartSystemUI(ctx);
    }

    private void updateClockName(int position) {
        String[] clockNames = {
            "Default Clock",
            "OnePlus Clock",
            "IOS Clock",
            "Simple Clock",
            "MIUI Clock",
            "IDE Clock",
            "Moto Clock",
            "Stylish Clock",
            "Stylish Clock 2",
            "Stylish Clock 3",
            "Stylish Clock 4",
            "Stylish Clock 5",
            "Stylish Clock 6",
            "Stylish Clock 7",
            "Stylish Clock 8",
            "Stylish Clock 9",
            "Stylish Clock 10",
            "Text Clock",
            "LifeStyle Clock",
            "Android 9 Vibe",
            "NothingOS 1 Clock",
            "NothingOS 2 Clock",
            "Stacked Clock",
            "X Factor",
            "Simple Analog",
            "Block",
            "Bubble",
            "Label Clock",
            "Taden Clock",
            "Mont Clock",
            "Encode Clock",
            "NOS Clock 3",
        };
        if (clockNameTextView != null && position >= 0 && position < clockNames.length) {
            clockNameTextView.setText(clockNames[position]);
        }
    }

    private void updateClockOverlays(int clockStyle) {
        mThemeUtils.setOverlayEnabled(
                "android.theme.customization.hideclock",
                clockStyle != 0 ? "com.android.systemui.clocks.hideclock" : "android",
                "android");
        mThemeUtils.setOverlayEnabled(
                "android.theme.customization.smartspace",
                clockStyle != 0 ? "com.android.systemui.hide.smartspace" : "com.android.systemui",
                "com.android.systemui");
    }

    private boolean isFirstTime() {
        return Settings.System.getIntForUser(
                getContext().getContentResolver(), PREF_FIRST_TIME, 1, UserHandle.USER_CURRENT) != 0;
    }

    private void disableHighlight() {
        Settings.System.putIntForUser(
                getContext().getContentResolver(), PREF_FIRST_TIME, 0, UserHandle.USER_CURRENT);
    }

    private class ClockPagerAdapter extends PagerAdapter {
        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View layout = inflater.inflate(CLOCK_LAYOUTS[position], container, false);
            container.addView(layout);
            return layout;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return CLOCK_LAYOUTS.length;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateClockName(mClockPosition);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateClockName(mClockPosition);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DERPFEST;
    }
}
