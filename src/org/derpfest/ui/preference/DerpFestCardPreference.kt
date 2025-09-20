/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.ui.preference

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import com.android.settingslib.widget.AdaptiveIcon
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.android.settings.R
import com.android.settings.Utils

open class DerpFestCardPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : Preference(context, attrs) {
    
    companion object {
        const val CORNERS_TOP = 0
        const val CORNERS_BOTTOM = 1
        const val CORNERS_BOTH = 2
        const val CORNERS_NONE = 3
    }
    
    private lateinit var holder: PreferenceViewHolder
    private lateinit var container: CardView
    private var cornerType: Int? = null
    private var cornerTypeString: String? = null
    private var mIconStyle: Int = 0
    private var mNormalColor: Int = 0
    private var mAccentColor: Int = 0
    
    init {
        CoroutineScope(Dispatchers.Main).launch {
            // Original had a Core.init() call here that we can skip
        }
        
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.DerpFestCardPreference)
            cornerTypeString = a.getString(R.styleable.DerpFestCardPreference_cornerType)
            a.recycle()
        }
        
        layoutResource = R.layout.derpfest_card_preference
        updateTheme()
    }
    
    private fun updateTheme() {
        val attrs = intArrayOf(
            android.R.attr.colorControlNormal,
            android.R.attr.colorAccent
        )
        val ta = context.theme.obtainStyledAttributes(attrs)
        mNormalColor = ta.getColor(0, 0xff808080.toInt())
        mAccentColor = ta.getColor(1, 0xff808080.toInt())
        ta.recycle()
        
        mIconStyle = Settings.System.getInt(
            context.contentResolver,
            Settings.System.THEMING_SETTINGS_DASHBOARD_ICONS, 0
        )
    }
    
    private fun themeIcon() {
        val iconDrawable = icon
        if (iconDrawable != null) {
            if (iconDrawable is AdaptiveIcon) {
                // Clear colors from previous calls
                iconDrawable.resetCustomColors()
                if (mIconStyle == 0) {
                    // Style 0: Use AOSP's natural theming - no custom colors applied
                    return
                }
                when (mIconStyle) {
                    1 -> iconDrawable.setCustomForegroundColor(context.getColor(android.R.color.white))
                    2 -> {
                        iconDrawable.setCustomBackgroundColor(mAccentColor)
                        iconDrawable.setCustomForegroundColor(context.getColor(android.R.color.white))
                    }
                    3 -> {
                        iconDrawable.setCustomForegroundColor(mNormalColor)
                        iconDrawable.setCustomBackgroundColor(0)
                    }
                    4 -> {
                        iconDrawable.setCustomForegroundColor(mAccentColor)
                        iconDrawable.setCustomBackgroundColor(0)
                    }
                }
            } else if (iconDrawable is LayerDrawable) {
                if (iconDrawable.numberOfLayers == 2) {
                    val fg = iconDrawable.getDrawable(1)
                    val bg = iconDrawable.getDrawable(0)
                    // Clear tints from previous calls
                    bg.setTintList(null)
                    fg.setTintList(null)
                    if (mIconStyle == 0) {
                        // Style 0: Use AOSP's natural theming - no custom colors applied
                        return
                    }
                    when (mIconStyle) {
                        1 -> fg.setTint(context.getColor(android.R.color.white))
                        2 -> {
                            bg.setTint(mAccentColor)
                            fg.setTint(context.getColor(android.R.color.white))
                        }
                        3 -> {
                            fg.setTint(mNormalColor)
                            bg.setTint(0)
                        }
                        4 -> {
                            fg.setTint(mAccentColor)
                            bg.setTint(0)
                        }
                    }
                }
            }
        }
    }
    
    private fun hideIcon() {
        holder.findViewById(R.id.icon_frame)?.visibility = View.GONE
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.marginStart = 0
        holder.findViewById(R.id.pref)?.layoutParams = params
    }
    
    private fun setupCorners() {
        // Convert cornerTypeString to cornerType integer
        if (cornerType == null && cornerTypeString != null) {
            cornerType = when (cornerTypeString) {
                "top" -> CORNERS_TOP
                "bottom" -> CORNERS_BOTTOM
                "both" -> CORNERS_BOTH
                "none" -> CORNERS_NONE
                else -> CORNERS_BOTH // Default
            }
        }
        
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        // Get current margins
        var topMargin = 0
        var bottomMargin = 0
        
        (container.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
            topMargin = it.topMargin
            bottomMargin = it.bottomMargin
        }
        
        // Apply drawable based on corner type
        when (cornerType) {
            CORNERS_TOP -> {
                container.setBackgroundResource(R.drawable.derpfest_card_background_rounded_top_selectable)
                bottomMargin = context.resources.getDimensionPixelSize(R.dimen.derpfest_card_margin_vertical_small)
            }
            CORNERS_BOTTOM -> {
                container.setBackgroundResource(R.drawable.derpfest_card_background_rounded_bottom_selectable)
                topMargin = context.resources.getDimensionPixelSize(R.dimen.derpfest_card_margin_vertical_small)
            }
            CORNERS_BOTH, null -> {
                container.setBackgroundResource(R.drawable.derpfest_card_background_selectable)
            }
            CORNERS_NONE -> {
                container.setBackgroundResource(R.drawable.derpfest_card_background_rounded_minimal_selectable)
                topMargin = context.resources.getDimensionPixelSize(R.dimen.derpfest_card_margin_vertical_small)
                bottomMargin = context.resources.getDimensionPixelSize(R.dimen.derpfest_card_margin_vertical_small)
            }
        }
        
        // Get current horizontal margins
        val leftMargin = (container.layoutParams as? ViewGroup.MarginLayoutParams)?.leftMargin ?: 0
        val rightMargin = (container.layoutParams as? ViewGroup.MarginLayoutParams)?.rightMargin ?: 0
        
        // Set all margins
        layoutParams.setMargins(leftMargin, topMargin, rightMargin, bottomMargin)
        holder.findViewById(R.id.container)?.layoutParams = layoutParams
    }
    
    @SuppressLint("RestrictedApi")
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        this.holder = holder
        
        // Update theme and apply icon theming
        updateTheme()
        themeIcon()
        
        // Remove background from the preference item
        holder.itemView.background = null
        
        // Set up the card container
        (holder.findViewById(R.id.container) as? CardView)?.also { cardView ->
            container = cardView
            cardView.setOnClickListener { performClick() }
            
            // If no icon is set, hide the icon frame
            if (icon == null) {
                hideIcon()
            }
            
            // Set up corners based on the specified type
            setupCorners()
        }
    }
} 
