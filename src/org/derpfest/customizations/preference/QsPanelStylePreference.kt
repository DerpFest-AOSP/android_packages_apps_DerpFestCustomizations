/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.android.settings.R
import com.android.settingslib.widget.theme.R as SettingsThemeR
import com.android.settingslib.widget.GroupSectionDividerMixin
import com.android.settingslib.widget.NormalPaddingMixin
import org.derpfest.support.preferences.SecureSettingsStore

/**
 * Visual picker for [Settings.Secure] `qs_panel_style`:
 * 0 = AOSP card tiles, 1 = classic circular tiles.
 */
class QsPanelStylePreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : Preference(context, attrs), GroupSectionDividerMixin, NormalPaddingMixin {

    /**
     * Last style the bound views were showing. Survives view-holder rebinds so a recreation can
     * still animate from the previous style instead of snapping.
     */
    private var visualCircular: Boolean? = null
    private var boundPreview: QsPanelStylePreviewView? = null
    private var boundToolbar: QsPanelStyleSegmentedToolbar? = null

    private val notifySiblings = Runnable {
        visualCircular = isCircular()
        callChangeListener(getPersistedString(DEFAULT_VALUE))
    }

    init {
        layoutResource = R.layout.qs_panel_style_preference
        isSelectable = false
        preferenceDataStore = SecureSettingsStore(context.contentResolver)
        if (title == null) {
            setTitle(R.string.qs_panel_style_title)
        }
        if (summary == null) {
            setSummary(R.string.qs_panel_style_summary)
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.isDividerAllowedAbove = false
        holder.isDividerAllowedBelow = false
        holder.itemView.background = null
        applyPreferenceTextColors(holder)

        val preview = holder.findViewById(R.id.qs_panel_style_preview) as QsPanelStylePreviewView
        val toolbar = holder.findViewById(R.id.qs_style_toolbar) as QsPanelStyleSegmentedToolbar
        boundPreview = preview
        boundToolbar = toolbar

        val circular = isCircular()
        val animate = visualCircular != null && visualCircular != circular
        preview.setCircular(circular, animate)
        toolbar.setCircular(circular, animate)
        if (!animate) {
            visualCircular = circular
        }

        toolbar.setOnStyleSelectedListener { selectCircular ->
            selectStyle(if (selectCircular) STYLE_CIRCULAR else STYLE_CARD)
        }
    }

    private fun selectStyle(style: Int) {
        val value = style.toString()
        if (getPersistedString(DEFAULT_VALUE) == value) return
        persistString(value)
        val circular = style == STYLE_CIRCULAR
        boundPreview?.setCircular(circular, animate = true)
        boundToolbar?.setCircular(circular, animate = true)
        // Showing/hiding sibling prefs rebinds this row and would snap the animation.
        boundToolbar?.removeCallbacks(notifySiblings)
        boundToolbar?.postDelayed(notifySiblings, SIBLING_UPDATE_DELAY_MS)
    }

    private fun applyPreferenceTextColors(holder: PreferenceViewHolder) {
        (holder.findViewById(android.R.id.title) as? TextView)?.apply {
            setTextAppearance(SettingsThemeR.style.TextAppearance_SettingsLib_PreferenceTitle)
            setTextColor(context.getColor(SettingsThemeR.color.settingslib_materialColorOnSurface))
        }
        (holder.findViewById(android.R.id.summary) as? TextView)?.apply {
            setTextAppearance(SettingsThemeR.style.TextAppearance_SettingsLib_PreferenceSummary)
            setTextColor(context.getColor(SettingsThemeR.color.settingslib_materialColorOnSurfaceVariant))
        }
    }

    private fun isCircular(): Boolean =
        getPersistedString(DEFAULT_VALUE) == STYLE_CIRCULAR.toString()

    companion object {
        const val STYLE_CARD = 0
        const val STYLE_CIRCULAR = 1
        private const val DEFAULT_VALUE = "0"
        private const val SIBLING_UPDATE_DELAY_MS = 360L
    }
}
