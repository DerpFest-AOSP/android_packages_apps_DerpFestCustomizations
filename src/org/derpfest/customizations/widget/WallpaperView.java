/*
 * SPDX-FileCopyrightText: 2021 Project Radiant
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.widget;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Shows the current system wallpaper with a light dim for preview UIs.
 */
public class WallpaperView extends ImageView {

    private final Context mContext;

    public WallpaperView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }

    public WallpaperView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public WallpaperView(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(mContext);
        Drawable wallpaperDrawable = wallpaperManager.getDrawable();

        if (wallpaperDrawable != null) {
            ColorDrawable dimOverlay = new ColorDrawable(Color.argb(51, 0, 0, 0));
            Drawable[] layers = new Drawable[]{wallpaperDrawable, dimOverlay};
            android.graphics.drawable.LayerDrawable layeredDrawable =
                    new android.graphics.drawable.LayerDrawable(layers);
            setImageDrawable(layeredDrawable);
        } else {
            setImageDrawable(null);
        }
    }
}
