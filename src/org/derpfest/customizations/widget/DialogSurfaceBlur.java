/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;

import com.android.internal.graphics.drawable.BackgroundBlurDrawable;
import com.android.settings.R;

import java.util.function.Consumer;

/**
 * Applies the Android 17 volume-panel surface blur: a per-view
 * {@link BackgroundBlurDrawable} with a Material surface-effect overlay.
 */
public final class DialogSurfaceBlur {

    private static final float DIM_AMOUNT_WITH_BLUR = 0.1f;
    private static final float DIM_AMOUNT_NO_BLUR = 0.4f;

    private final Context mContext;
    private final float mCornerRadius;
    private final int mBlurRadiusPx;

    private AlertDialog mDialog;
    private BackgroundBlurDrawable mBlurDrawable;
    private GradientDrawable mOverlay;
    private Consumer<Boolean> mBlurEnabledListener;

    public DialogSurfaceBlur(Context context) {
        mContext = context;
        mCornerRadius = context.getResources().getDimension(
                R.dimen.dialog_surface_corner_radius);
        mBlurRadiusPx = context.getResources().getDimensionPixelSize(
                R.dimen.dialog_surface_blur_radius);
    }

    public void attach(AlertDialog dialog) {
        mDialog = dialog;
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setBackgroundBlurRadius(0);

        View decor = window.getDecorView();
        if (decor.getViewRootImpl() != null) {
            apply(window, decor);
            return;
        }
        decor.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                v.removeOnAttachStateChangeListener(this);
                if (mDialog != null) {
                    apply(window, v);
                }
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
            }
        });
    }

    public void detach() {
        if (mBlurEnabledListener != null && mDialog != null) {
            Window window = mDialog.getWindow();
            if (window != null) {
                window.getWindowManager().removeCrossWindowBlurEnabledListener(
                        mBlurEnabledListener);
            }
        }
        mBlurEnabledListener = null;
        mBlurDrawable = null;
        mOverlay = null;
        mDialog = null;
    }

    private void apply(Window window, View decor) {
        if (decor.getViewRootImpl() == null) {
            return;
        }

        mBlurDrawable = decor.getViewRootImpl().createBackgroundBlurDrawable();
        mBlurDrawable.setCornerRadius(mCornerRadius);
        mBlurDrawable.setBlurRadius(0);

        Drawable overlayDrawable = mContext.getDrawable(
                R.drawable.dialog_qs_tile_icon_shape_window_background);
        if (overlayDrawable instanceof GradientDrawable) {
            mOverlay = (GradientDrawable) overlayDrawable.mutate();
            mOverlay.setCornerRadius(mCornerRadius);
        } else {
            mOverlay = new GradientDrawable();
            mOverlay.setShape(GradientDrawable.RECTANGLE);
            mOverlay.setCornerRadius(mCornerRadius);
        }

        decor.setBackground(new LayerDrawable(new Drawable[] { mBlurDrawable, mOverlay }));
        decor.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), mCornerRadius);
            }
        });
        decor.setClipToOutline(true);

        mBlurEnabledListener = this::updateForBlur;
        window.getWindowManager().addCrossWindowBlurEnabledListener(mBlurEnabledListener);
        updateForBlur(window.getWindowManager().isCrossWindowBlurEnabled());
    }

    private void updateForBlur(boolean blursEnabled) {
        if (mDialog == null || mBlurDrawable == null || mOverlay == null) {
            return;
        }
        Window window = mDialog.getWindow();
        if (window == null) {
            return;
        }

        mBlurDrawable.setBlurRadius(blursEnabled ? mBlurRadiusPx : 0);
        mOverlay.setColor(mContext.getColor(blursEnabled
                ? com.android.internal.R.color.customColorSurfaceEffect0
                : com.android.internal.R.color.customColorSurfaceEffect0Fallback));
        window.setDimAmount(blursEnabled ? DIM_AMOUNT_WITH_BLUR : DIM_AMOUNT_NO_BLUR);
        window.setAttributes(window.getAttributes());
    }
}
