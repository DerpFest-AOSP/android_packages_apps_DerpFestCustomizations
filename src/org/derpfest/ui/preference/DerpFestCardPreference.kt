/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.ui.preference

import android.annotation.SuppressLint
import android.content.Context
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
