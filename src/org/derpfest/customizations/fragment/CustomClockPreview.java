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
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.derpfest.ThemeUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.widget.LayoutPreference;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import org.derpfest.customizations.utils.SystemUiUtils;

public class CustomClockPreview extends SettingsPreferenceFragment {

    private static final String PREF_FIRST_TIME = "first_time_clock_face_access";

    private static final String KEY_CLOCK_PREVIEW = "clock_preview";

    /** Inclusive range per section: System, Stylish, Text &amp; numbers, Analog &amp; more. */
    private static final int[] SECTION_START = {0, 7, 17, 24};
    private static final int[] SECTION_END = {6, 16, 23, 31};

    private ViewPager viewPager;
    private TabLayout sectionTabs;
    private ClockPagerAdapter pagerAdapter;
    private ExtendedFloatingActionButton applyFab;
    private View highlightGuide;
    private TextView clockNameTextView;

    private int mClockPosition = 0;
    private String[] mClockNames = new String[0];
    private boolean mSyncingTabFromViewPager;

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

        requireActivity().setTitle(getString(R.string.lockscreen_custom_clock_style_title));
        mThemeUtils = new ThemeUtils(requireActivity());

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
        if (clockPreviewPref == null) {
            return;
        }

        clockNameTextView = clockPreviewPref.findViewById(R.id.clock_name);
        viewPager = clockPreviewPref.findViewById(R.id.view_pager);
        sectionTabs = clockPreviewPref.findViewById(R.id.clock_section_tabs);
        applyFab = clockPreviewPref.findViewById(R.id.apply_extended_fab);
        highlightGuide = clockPreviewPref.findViewById(R.id.highlight_guide);

        mClockNames = getResources().getStringArray(R.array.lockscreen_clock_names);
        if (mClockNames.length != CLOCK_LAYOUTS.length) {
            throw new IllegalStateException(
                    "lockscreen_clock_names count must match CLOCK_LAYOUTS (" + CLOCK_LAYOUTS.length + ")");
        }

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

        if (sectionTabs != null) {
            // TabLayout’s internal strip clips pill backgrounds during horizontal overscroll.
            sectionTabs.setClipToPadding(false);
            sectionTabs.setClipChildren(false);
            if (sectionTabs.getChildCount() > 0) {
                View strip = sectionTabs.getChildAt(0);
                if (strip instanceof ViewGroup) {
                    ((ViewGroup) strip).setClipChildren(false);
                }
            }
            sectionTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    onSectionTabInteracted(tab.getPosition());
                    if (sectionTabs != null) {
                        refreshClockTabLabelColors(tab.getPosition());
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) { }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    onSectionTabInteracted(tab.getPosition());
                }
            });
            syncTabFromViewPager(mClockPosition);
            applyCenteredCustomTabLabels(sectionTabs);
        }

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
            public void onPageScrollStateChanged(int state) { }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                mClockPosition = position;
                if (viewPager != null) {
                    viewPager.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                }
                syncTabFromViewPager(position);
                updateClockName(position);
            }
        });

        updateClockName(mClockPosition);
    }

    /**
     * One centered label per tab. ({@code TabLayout#getTabTextColors()} state lists do not line up
     * with custom {@link TextView} views, which made labels effectively invisible; we set
     * {@link #refreshClockTabLabelColors} explicitly to match the XML tab colors.
     */
    private void applyCenteredCustomTabLabels(TabLayout tabLayout) {
        int horizontalPaddingPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 8, tabLayout.getResources().getDisplayMetrics());
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab == null) {
                continue;
            }
            CharSequence tabTitle = tab.getText();
            if (TextUtils.isEmpty(tabTitle)) {
                continue;
            }
            TextView label = new TextView(tabLayout.getContext());
            label.setText(tabTitle);
            label.setSingleLine(true);
            label.setEllipsize(TextUtils.TruncateAt.END);
            label.setGravity(Gravity.CENTER);
            label.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            label.setIncludeFontPadding(false);
            label.setPadding(horizontalPaddingPx, 0, horizontalPaddingPx, 0);
            label.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            tab.setCustomView(label);
        }
        tabLayout.post(() -> {
            for (int i = 0; i < tabLayout.getTabCount(); i++) {
                TabLayout.Tab tab = tabLayout.getTabAt(i);
                if (tab == null) {
                    continue;
                }
                View v = tab.getCustomView();
                if (v == null) {
                    continue;
                }
                ViewGroup.LayoutParams p = v.getLayoutParams();
                if (p != null) {
                    p.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    p.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    v.setLayoutParams(p);
                }
            }
            tabLayout.requestLayout();
        });
        Context ctx = getContext();
        if (ctx != null) {
            int section = getSectionForPosition(mClockPosition);
            applyClockTabLabelTextColorsForSection(ctx, tabLayout, section);
        }
    }

    private void refreshClockTabLabelColors(int selectedSectionIndex) {
        Context ctx = getContext();
        if (ctx == null || sectionTabs == null) {
            return;
        }
        applyClockTabLabelTextColorsForSection(ctx, sectionTabs, selectedSectionIndex);
    }

    private static void applyClockTabLabelTextColorsForSection(
            Context context, TabLayout tabLayout, int selectedSectionIndex) {
        @ColorInt int selected = ContextCompat.getColor(
                context, R.color.lockscreen_clock_tab_text_selected);
        @ColorInt int normal = ContextCompat.getColor(context, R.color.colorOnSurfaceVariant);
        for (int s = 0; s < tabLayout.getTabCount(); s++) {
            TabLayout.Tab t = tabLayout.getTabAt(s);
            if (t == null) {
                continue;
            }
            View cv = t.getCustomView();
            if (!(cv instanceof TextView)) {
                continue;
            }
            ((TextView) cv).setTextColor(s == selectedSectionIndex ? selected : normal);
        }
    }

    private void onSectionTabInteracted(int sectionIndex) {
        if (mSyncingTabFromViewPager) {
            return;
        }
        int target = getFirstIndexInSection(sectionIndex);
        if (mClockPosition != target) {
            viewPager.setCurrentItem(target, true);
        }
    }

    private void syncTabFromViewPager(int position) {
        if (sectionTabs == null) {
            return;
        }
        int section = getSectionForPosition(position);
        TabLayout.Tab tab = sectionTabs.getTabAt(section);
        if (tab == null) {
            return;
        }
        mSyncingTabFromViewPager = true;
        sectionTabs.selectTab(tab, true);
        mSyncingTabFromViewPager = false;
        refreshClockTabLabelColors(getSectionForPosition(position));
    }

    private static int getSectionForPosition(int position) {
        if (position <= SECTION_END[0]) {
            return 0;
        }
        if (position <= SECTION_END[1]) {
            return 1;
        }
        if (position <= SECTION_END[2]) {
            return 2;
        }
        return 3;
    }

    private static int getFirstIndexInSection(int section) {
        if (section < 0 || section >= SECTION_START.length) {
            return 0;
        }
        return SECTION_START[section];
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
        if (clockNameTextView != null
                && position >= 0 && position < mClockNames.length) {
            clockNameTextView.setText(mClockNames[position]);
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
