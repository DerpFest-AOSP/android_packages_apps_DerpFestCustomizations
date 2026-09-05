/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.preference;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.PathParser;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.ColorUtils;
import androidx.preference.Preference;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

import org.derpfest.customizations.widget.DialogSurfaceBlur;

/**
 * Dialog with a preview of each classic QS tile icon mask shape. Persists {@code
 * Settings.Secure#qs_tile_icon_shape} (string key), matching SystemUI {@code QSTileIconShapes}.
 */
public class QsTileIconShapePreference extends Preference {

    private static final String SETTING_KEY = "qs_tile_icon_shape";

    /**
     * Solid fill behind ring in list preview for outline_style_dark when QS tile gradient is off
     * (matches SystemUI surface fill).
     */
    private static final int OUTLINE_DARK_PREVIEW_BACKDROP_ARGB = 0xFF2C2C2E;

    /** Matches {@code CommonTileDefaults.ClassicOutlineDarkGradientWashAlpha} in SystemUI. */
    private static final float OUTLINE_DARK_PREVIEW_GRADIENT_WASH_ALPHA = 0.22f;

    private DialogSurfaceBlur mSurfaceBlur;
    private AlertDialog mDialog;
    private boolean mDialogConfirmed;
    private String mOriginalValue;

    private String[] mEntries;
    private String[] mEntryValues;

    public QsTileIconShapePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public QsTileIconShapePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mEntries = context.getResources().getStringArray(R.array.qs_tile_icon_shape_entries);
        mEntryValues = context.getResources().getStringArray(R.array.qs_tile_icon_shape_values);
    }

    private boolean isQsTileGradientEnabled() {
        try {
            return Settings.System.getIntForUser(
                            getContext().getContentResolver(),
                            Settings.System.QS_TILE_GRADIENT_ENABLED,
                            1,
                            UserHandle.USER_CURRENT)
                    == 1;
        } catch (Throwable t) {
            return true;
        }
    }

    private int readGradientStartArgbSetting() {
        try {
            return Settings.System.getIntForUser(
                    getContext().getContentResolver(),
                    Settings.System.GRADIENT_START_COLOR,
                    0,
                    UserHandle.USER_CURRENT);
        } catch (Throwable t) {
            return 0;
        }
    }

    private int readGradientEndArgbSetting() {
        try {
            return Settings.System.getIntForUser(
                    getContext().getContentResolver(),
                    Settings.System.GRADIENT_END_COLOR,
                    0,
                    UserHandle.USER_CURRENT);
        } catch (Throwable t) {
            return 0;
        }
    }

    /**
     * Same RGB-only convention as SystemUI {@code gradientSettingArgbToColor}: alpha 0 in settings
     * means full-opacity RGB.
     */
    private static int normalizeQsGradientStopArgb(int settingArgb, int fallbackArgb) {
        if (settingArgb == 0) {
            return fallbackArgb;
        }
        int a = (settingArgb >>> 24) & 0xFF;
        if (a == 0) {
            return (0xFF << 24) | (settingArgb & 0x00FFFFFF);
        }
        return settingArgb;
    }

    private int[] resolveOutlineDarkGradientStopsForPreview(int themeColor) {
        int fallbackStart = ColorUtils.blendARGB(themeColor, Color.WHITE, 0.25f);
        int fallbackEnd = ColorUtils.blendARGB(themeColor, Color.BLACK, 0.35f);
        return new int[] {
            normalizeQsGradientStopArgb(readGradientStartArgbSetting(), fallbackStart),
            normalizeQsGradientStopArgb(readGradientEndArgbSetting(), fallbackEnd)
        };
    }

    private Drawable createPreviewDrawable(String shapeKey, int color) {
        if ("just_icons".equals(shapeKey)) {
            Drawable d = getContext().getDrawable(R.drawable.ic_signal_flashlight);
            if (d != null) {
                d = d.mutate();
                d.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
                return d;
            }
            // Missing on some variants: fall back to mask path preview.
        }
        String pathData = QsTileIconShapePathData.pathDataForPreview(shapeKey);
        float viewBox = QsTileIconShapePathData.viewBoxForPreview(shapeKey);
        float strokeFrac = QsTileIconShapePathData.previewStrokeFractionFor(shapeKey);
        if ("outline_style_dark".equals(shapeKey)) {
            if (isQsTileGradientEnabled()) {
                int[] stops = resolveOutlineDarkGradientStopsForPreview(color);
                return new TileIconShapePreviewDrawable(
                        pathData,
                        viewBox,
                        color,
                        strokeFrac,
                        0,
                        true,
                        stops[0],
                        stops[1]);
            }
            return new TileIconShapePreviewDrawable(
                    pathData,
                    viewBox,
                    color,
                    strokeFrac,
                    OUTLINE_DARK_PREVIEW_BACKDROP_ARGB,
                    false,
                    0,
                    0);
        }
        return new TileIconShapePreviewDrawable(
                pathData, viewBox, color, strokeFrac, 0, false, 0, 0);
    }

    private String getCurrentShapeKey() {
        String raw = Settings.Secure.getString(getContext().getContentResolver(), SETTING_KEY);
        if (raw == null) {
            return QsTileIconShapePathData.DEFAULT_KEY;
        }
        if (!QsTileIconShapePathData.isKnownKey(raw)) {
            // Removed shapes: migrate stored value to default.
            if ("pokesign".equals(raw) || "ninja".equals(raw)) {
                Settings.Secure.putString(
                        getContext().getContentResolver(), SETTING_KEY,
                        QsTileIconShapePathData.DEFAULT_KEY);
            }
            return QsTileIconShapePathData.DEFAULT_KEY;
        }
        return raw;
    }

    private void updateSummary() {
        String key = getCurrentShapeKey();
        int index = indexOfValue(key);
        setSummary(index >= 0 ? mEntries[index] : mEntries[0]);
    }

    private int indexOfValue(String value) {
        for (int i = 0; i < mEntryValues.length; i++) {
            if (mEntryValues[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }

    private void applyStyleValue(int position) {
        Settings.Secure.putString(getContext().getContentResolver(), SETTING_KEY,
                mEntryValues[position]);
    }

    private void clearDialogSolidBackgrounds(View root) {
        View grid = root.findViewById(R.id.style_picker_grid);
        View ourContentRoot = (grid != null && grid.getParent() instanceof View)
                ? (View) grid.getParent() : null;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                clearOpaqueBackgroundsRecursive(group.getChildAt(i), ourContentRoot);
            }
        }
    }

    private void clearOpaqueBackgroundsRecursive(View view, View excludeSubtree) {
        if (view == excludeSubtree) return;
        if (view instanceof android.widget.Button) return;
        view.setBackgroundResource(android.R.color.transparent);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                clearOpaqueBackgroundsRecursive(group.getChildAt(i), excludeSubtree);
            }
        }
    }

    @Override
    protected void onAttachedToHierarchy(androidx.preference.PreferenceManager pm) {
        super.onAttachedToHierarchy(pm);
        updateSummary();
    }

    @Override
    protected void onClick() {
        View view = View.inflate(getContext(), R.layout.dialog_qs_tile_icon_shape, null);
        RecyclerView grid = view.findViewById(R.id.style_picker_grid);

        mOriginalValue = getCurrentShapeKey();
        mDialogConfirmed = false;
        int selectedIndex = indexOfValue(mOriginalValue);
        if (selectedIndex < 0) selectedIndex = 0;

        grid.setLayoutManager(new GridLayoutManager(getContext(), 4));
        grid.setHasFixedSize(true);
        int gap = getContext().getResources().getDimensionPixelSize(
                com.android.settingslib.widget.theme.R.dimen
                        .settingslib_expressive_space_extrasmall2);
        grid.addItemDecoration(new GridSpacingDecoration(gap));
        TileIconShapeAdapter adapter = new TileIconShapeAdapter(
                getContext(), mEntries, mEntryValues, this, selectedIndex);
        adapter.setOnStyleClickListener(position -> {
            adapter.setSelectedIndex(position);
            applyStyleValue(position);
        });
        grid.setAdapter(adapter);
        grid.scrollToPosition(selectedIndex);

        AlertDialog.Builder builder =
                new AlertDialog.Builder(getContext(), R.style.QsTileIconShapeDialogTheme);
        builder.setTitle(getTitle());
        builder.setView(view);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            mDialogConfirmed = true;
            String value = getCurrentShapeKey();
            updateSummary();
            callChangeListener(value);
        });

        mDialog = builder.create();
        Window window = mDialog.getWindow();
        if (window != null) {
            float density = getContext().getResources().getDisplayMetrics().density;
            int maxWidthPx = (int) (400 * density + 0.5f);
            int screenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = Math.min(maxWidthPx, (int) (screenWidth * 0.92f));
            window.setAttributes(lp);
        }
        mSurfaceBlur = new DialogSurfaceBlur(getContext());
        mDialog.setOnShowListener(dialog -> {
            if (mSurfaceBlur != null) {
                mSurfaceBlur.attach(mDialog);
            }
            Window w = ((AlertDialog) dialog).getWindow();
            if (w != null) clearDialogSolidBackgrounds(w.getDecorView());
        });
        mDialog.setOnDismissListener(dialog -> {
            if (!mDialogConfirmed) {
                Settings.Secure.putString(getContext().getContentResolver(), SETTING_KEY,
                        mOriginalValue);
            }
            if (mSurfaceBlur != null) {
                mSurfaceBlur.detach();
                mSurfaceBlur = null;
            }
            mDialog = null;
        });

        mDialog.show();
    }

    private static class GridSpacingDecoration extends RecyclerView.ItemDecoration {
        private final int mSpacing;

        GridSpacingDecoration(int spacingPx) {
            mSpacing = spacingPx;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            outRect.set(mSpacing, mSpacing, mSpacing, mSpacing);
        }
    }

    private interface OnStyleClickListener {
        void onStyleClicked(int position);
    }

    private static final class TileIconShapePreviewDrawable extends Drawable {
        private final float mViewBox;
        private final float mStrokeFraction;
        private final int mStrokeColor;
        /** 0 = no fill (stroke-only or solid fill preview). */
        private final int mBackdropArgb;
        /** When true, draw low-alpha vertical gradient wash (QS gradient on). */
        private final boolean mOutlineDarkGradientWash;
        private final int mGradientStartArgb;
        private final int mGradientEndArgb;

        private final Path mPath;
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        TileIconShapePreviewDrawable(
                String pathData,
                float viewBox,
                int color,
                float strokeFraction,
                int backdropArgb,
                boolean outlineDarkGradientWash,
                int gradientStartArgb,
                int gradientEndArgb) {
            mViewBox = viewBox > 0f ? viewBox : 100f;
            mStrokeFraction = strokeFraction;
            mStrokeColor = color;
            mBackdropArgb = backdropArgb;
            mOutlineDarkGradientWash = outlineDarkGradientWash;
            mGradientStartArgb = gradientStartArgb;
            mGradientEndArgb = gradientEndArgb;
            Path path;
            try {
                path = PathParser.createPathFromPathData(pathData);
            } catch (RuntimeException e) {
                path = PathParser.createPathFromPathData(
                        QsTileIconShapePathData.pathStringForKey(
                                QsTileIconShapePathData.DEFAULT_KEY));
            }
            mPath = path;
            if (strokeFraction > 0f) {
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setStrokeJoin(Paint.Join.ROUND);
                mPaint.setStrokeCap(Paint.Cap.ROUND);
            } else {
                mPaint.setStyle(Paint.Style.FILL);
            }
            mPaint.setColor(color);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            if (b.isEmpty()) return;
            canvas.save();
            float sx = b.width() / mViewBox;
            float sy = b.height() / mViewBox;
            canvas.translate(b.left, b.top);
            canvas.scale(sx, sy);
            if (mOutlineDarkGradientWash) {
                mPaint.setStyle(Paint.Style.FILL);
                // Align with Compose Brush.linearGradient(colors): top-left → bottom-right in tile.
                Shader shader =
                        new LinearGradient(
                                0,
                                0,
                                mViewBox,
                                mViewBox,
                                mGradientStartArgb,
                                mGradientEndArgb,
                                Shader.TileMode.CLAMP);
                mPaint.setShader(shader);
                mPaint.setAlpha(
                        Math.round(OUTLINE_DARK_PREVIEW_GRADIENT_WASH_ALPHA * 255f));
                canvas.drawPath(mPath, mPaint);
                mPaint.setShader(null);
                mPaint.setAlpha(255);
            } else if (mBackdropArgb != 0) {
                mPaint.setStyle(Paint.Style.FILL);
                mPaint.setColor(mBackdropArgb);
                mPaint.setAlpha(255);
                canvas.drawPath(mPath, mPaint);
            }
            mPaint.setColor(mStrokeColor);
            if (mStrokeFraction > 0f) {
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setStrokeJoin(Paint.Join.ROUND);
                mPaint.setStrokeCap(Paint.Cap.ROUND);
                float minPx = Math.min(b.width(), b.height());
                float strokePath = (minPx * mStrokeFraction) / Math.min(sx, sy);
                mPaint.setStrokeWidth(strokePath);
            } else {
                mPaint.setStyle(Paint.Style.FILL);
            }
            canvas.drawPath(mPath, mPaint);
            canvas.restore();
        }

        @Override
        public void setAlpha(int alpha) {
            mPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            mPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    private static class TileIconShapeAdapter
            extends RecyclerView.Adapter<TileIconShapeAdapter.Holder> {
        private final LayoutInflater mInflater;
        private final String[] mEntries;
        private final String[] mEntryValues;
        private final QsTileIconShapePreference mPreference;
        private int mSelectedIndex;
        private final ColorStateList mIconColors;
        private OnStyleClickListener mListener;

        TileIconShapeAdapter(Context context, String[] entries, String[] entryValues,
                QsTileIconShapePreference preference, int selectedIndex) {
            mInflater = LayoutInflater.from(context);
            mEntries = entries;
            mEntryValues = entryValues;
            mPreference = preference;
            mSelectedIndex = selectedIndex;
            mIconColors = context.getColorStateList(R.color.style_picker_tile_icon);
        }

        void setOnStyleClickListener(OnStyleClickListener listener) {
            mListener = listener;
        }

        void setSelectedIndex(int index) {
            if (index == mSelectedIndex) {
                return;
            }
            int oldIndex = mSelectedIndex;
            mSelectedIndex = index;
            if (oldIndex >= 0) {
                notifyItemChanged(oldIndex);
            }
            notifyItemChanged(mSelectedIndex);
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(mInflater.inflate(R.layout.qs_tile_icon_shape_list_item, parent,
                    false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            boolean selected = position == mSelectedIndex;
            holder.itemView.setSelected(selected);
            holder.itemView.setContentDescription(mEntries[position]);
            String key = mEntryValues[position];
            int color = mIconColors.getColorForState(
                    selected
                            ? new int[] { android.R.attr.state_selected }
                            : new int[] {},
                    mIconColors.getDefaultColor());
            Drawable d = mPreference.createPreviewDrawable(key, color);
            holder.icon.setImageDrawable(d);
            holder.icon.setVisibility(d != null ? View.VISIBLE : View.GONE);
            holder.itemView.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && mListener != null) {
                    mListener.onStyleClicked(pos);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mEntries.length;
        }

        static class Holder extends RecyclerView.ViewHolder {
            final ImageView icon;

            Holder(@NonNull View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.qs_tile_icon_shape_preview);
            }
        }
    }
}
