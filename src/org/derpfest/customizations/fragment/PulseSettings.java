/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import org.derpfest.customizations.utils.DeviceUtils;
import org.derpfest.support.colorpicker.ColorPickerPreference;

public class PulseSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_PULSE_RENDERER = "pulse_renderer";
    private static final String KEY_PULSE_ROUNDED_BARS = "pulse_rounded_bars";
    private static final String KEY_PULSE_COLOR = "pulse_color";
    private static final String KEY_PULSE_CUSTOM_COLOR = "pulse_custom_color";
    private static final String KEY_PULSE_BAR_COUNT = "pulse_bar_count";
    private static final String KEY_PULSE_HAPTICS = "pulse_haptics_enabled";
    private static final String COLOR_CUSTOM = "custom";
    private static final String RENDERER_SOLID = "solid";
    private static final String RENDERER_RETRO = "retro";
    private static final String RENDERER_PARTICLE = "particle";

    private ListPreference mPulseRenderer;
    private Preference mPulseRoundedBars;
    private ListPreference mPulseColor;
    private ColorPickerPreference mPulseCustomColor;
    private Preference mPulseBarCount;
    private Preference mPulseHaptics;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pulse_settings);

        mPulseRenderer = findPreference(KEY_PULSE_RENDERER);
        mPulseRoundedBars = findPreference(KEY_PULSE_ROUNDED_BARS);
        mPulseColor = findPreference(KEY_PULSE_COLOR);
        mPulseCustomColor = findPreference(KEY_PULSE_CUSTOM_COLOR);
        mPulseBarCount = findPreference(KEY_PULSE_BAR_COUNT);
        mPulseHaptics = findPreference(KEY_PULSE_HAPTICS);

        if (mPulseHaptics != null && !DeviceUtils.hasVibrator(getContext())) {
            mPulseHaptics.setVisible(false);
        }

        if (mPulseRenderer != null) {
            mPulseRenderer.setOnPreferenceChangeListener(this);
        }
        if (mPulseColor != null) {
            mPulseColor.setOnPreferenceChangeListener(this);
        }
        updatePreferenceVisibility(
                mPulseRenderer != null ? mPulseRenderer.getValue() : null,
                mPulseColor != null ? mPulseColor.getValue() : null);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mPulseRenderer) {
            updatePreferenceVisibility((String) newValue,
                    mPulseColor != null ? mPulseColor.getValue() : null);
        } else if (preference == mPulseColor) {
            updatePreferenceVisibility(
                    mPulseRenderer != null ? mPulseRenderer.getValue() : null,
                    (String) newValue);
        }
        return true;
    }

    /**
     * Rounding is only visibly applied by SolidLine in SystemUI.
     * Retro VU uses hardcoded segment colors and ignores pulse_color.
     * Particle does not draw bars, so bar count has no visible effect.
     */
    private void updatePreferenceVisibility(String rendererValue, String colorValue) {
        if (rendererValue == null) {
            return;
        }

        boolean supportsRounding = RENDERER_SOLID.equals(rendererValue);
        boolean supportsColoring = !RENDERER_RETRO.equals(rendererValue);
        boolean supportsBarCount = !RENDERER_PARTICLE.equals(rendererValue);
        boolean customSelected = COLOR_CUSTOM.equals(colorValue);

        if (mPulseRoundedBars != null) {
            mPulseRoundedBars.setVisible(supportsRounding);
        }
        if (mPulseColor != null) {
            mPulseColor.setVisible(supportsColoring);
        }
        if (mPulseBarCount != null) {
            mPulseBarCount.setVisible(supportsBarCount);
        }
        updateCustomColorAvailability(supportsColoring, customSelected);
    }

    private void updateCustomColorAvailability(boolean supportsColoring, boolean customSelected) {
        if (mPulseCustomColor == null) {
            return;
        }
        mPulseCustomColor.setVisible(supportsColoring);
        mPulseCustomColor.setEnabled(customSelected);
        if (!supportsColoring) {
            return;
        }
        if (customSelected) {
            mPulseCustomColor.setAutoSummaryEnabled(true);
            final int color = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.PULSE_CUSTOM_COLOR, Color.WHITE);
            mPulseCustomColor.setSummary(ColorPickerPreference.convertToARGB(color));
        } else {
            mPulseCustomColor.setAutoSummaryEnabled(false);
            mPulseCustomColor.setSummary(R.string.pulse_custom_color_requires_custom);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DERPFEST;
    }
}
