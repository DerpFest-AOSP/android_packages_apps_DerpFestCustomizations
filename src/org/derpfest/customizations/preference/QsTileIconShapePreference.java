/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.preference;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.PathParser;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.android.settings.R;

import java.util.function.Consumer;

/**
 * Dialog with a preview of each classic QS tile icon mask shape. Persists {@code
 * Settings.Secure#qs_tile_icon_shape} (string key), matching SystemUI {@code QSTileIconShapes}.
 */
public class QsTileIconShapePreference extends Preference {

    private static final String SETTING_KEY = "qs_tile_icon_shape";

    private static final int BACKGROUND_BLUR_RADIUS = 80;
    private static final int WINDOW_BG_ALPHA_WITH_BLUR = 105;
    private static final int WINDOW_BG_ALPHA_NO_BLUR = 255;
    private static final float DIM_AMOUNT_WITH_BLUR = 0.1f;
    private static final float DIM_AMOUNT_NO_BLUR = 0.4f;

    private Drawable mWindowBackgroundDrawable;
    private Drawable mDecorBackgroundDrawable;
    private Consumer<Boolean> mBlurEnabledListener;
    private AlertDialog mDialog;

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

    private int getThemeIconColor() {
        TypedValue tv = new TypedValue();
        if (getContext().getTheme().resolveAttribute(android.R.attr.colorControlNormal, tv, true)) {
            if (tv.resourceId != 0) {
                return getContext().getColor(tv.resourceId);
            }
            return tv.data;
        }
        if (getContext().getTheme().resolveAttribute(android.R.attr.textColorPrimary, tv, true)) {
            if (tv.resourceId != 0) {
                return getContext().getColor(tv.resourceId);
            }
            return tv.data;
        }
        return 0xff000000;
    }

    private Drawable createPreviewDrawable(String shapeKey) {
        if ("just_icons".equals(shapeKey)) {
            Drawable d = getContext().getDrawable(R.drawable.ic_signal_flashlight);
            if (d != null) {
                d = d.mutate();
                d.setColorFilter(
                        new PorterDuffColorFilter(getThemeIconColor(), PorterDuff.Mode.SRC_IN));
                return d;
            }
            // Missing on some variants: fall back to mask path preview.
        }
        String pathData = QsTileIconShapePathData.pathDataForPreview(shapeKey);
        float viewBox = QsTileIconShapePathData.viewBoxForPreview(shapeKey);
        float strokeFrac = QsTileIconShapePathData.previewStrokeFractionFor(shapeKey);
        return new TileIconShapePreviewDrawable(pathData, viewBox, getThemeIconColor(), strokeFrac);
    }

    private String getCurrentShapeKey() {
        String raw = Settings.Secure.getString(getContext().getContentResolver(), SETTING_KEY);
        if (raw == null || !QsTileIconShapePathData.isKnownKey(raw)) {
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

    private void clearDialogSolidBackgrounds(View root) {
        View listView = root.findViewById(R.id.qs_tile_icon_shape_list);
        View ourContentRoot = (listView != null && listView.getParent() instanceof View
                && ((View) listView.getParent()).getParent() instanceof View)
                ? (View) ((View) listView.getParent()).getParent() : null;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                clearOpaqueBackgroundsRecursive(group.getChildAt(i), ourContentRoot);
            }
        }
    }

    private void clearOpaqueBackgroundsRecursive(View view, View excludeSubtree) {
        if (view == excludeSubtree) return;
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
        ListView listView = view.findViewById(R.id.qs_tile_icon_shape_list);

        String currentKey = getCurrentShapeKey();
        int selectedIndex = indexOfValue(currentKey);
        if (selectedIndex < 0) selectedIndex = 0;

        listView.setAdapter(new TileIconShapeAdapter(
                getContext(), mEntries, mEntryValues, this, selectedIndex));
        listView.setOnItemClickListener((parent, v, position, id) -> {
            String value = mEntryValues[position];
            Settings.Secure.putString(getContext().getContentResolver(), SETTING_KEY, value);
            setSummary(mEntries[position]);
            callChangeListener(value);
            if (mDialog != null) {
                mDialog.dismiss();
            }
        });

        AlertDialog.Builder builder =
                new AlertDialog.Builder(getContext(), R.style.QsTileIconShapeDialogTheme);
        builder.setTitle(getTitle());
        builder.setView(view);
        builder.setNegativeButton(android.R.string.cancel, null);

        mDialog = builder.create();
        Window window = mDialog.getWindow();
        if (window != null) {
            float density = getContext().getResources().getDisplayMetrics().density;
            int maxWidthPx = (int) (320 * density + 0.5f);
            int screenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = Math.min(maxWidthPx, (int) (screenWidth * 0.85f));
            window.setAttributes(lp);
            setupWindowBlur(window);
        }
        mDialog.setOnShowListener(dialog -> {
            TypedValue tv = new TypedValue();
            int accent = 0;
            if (getContext().getTheme().resolveAttribute(android.R.attr.colorAccent, tv, true)) {
                accent = tv.resourceId != 0 ? getContext().getColor(tv.resourceId) : tv.data;
            }
            if (accent != 0) {
                android.widget.Button negativeButton =
                        ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
                if (negativeButton != null) negativeButton.setTextColor(accent);
                int titleId = getContext().getResources().getIdentifier("alertTitle", "id", "android");
                TextView titleView = titleId != 0
                        ? (TextView) ((AlertDialog) dialog).getWindow().getDecorView()
                                .findViewById(titleId)
                        : null;
                if (titleView != null) titleView.setTextColor(accent);
            }
            Window w = ((AlertDialog) dialog).getWindow();
            if (w != null) clearDialogSolidBackgrounds(w.getDecorView());
        });
        mDialog.setOnDismissListener(dialog -> {
            if (mBlurEnabledListener != null && mDialog != null) {
                Window w = mDialog.getWindow();
                if (w != null) {
                    w.getWindowManager().removeCrossWindowBlurEnabledListener(mBlurEnabledListener);
                }
            }
            mBlurEnabledListener = null;
            mWindowBackgroundDrawable = null;
            mDecorBackgroundDrawable = null;
            mDialog = null;
        });

        mDialog.show();
    }

    private void setupWindowBlur(Window window) {
        if (window == null) return;
        mWindowBackgroundDrawable =
                getContext().getDrawable(R.drawable.dialog_qs_tile_icon_shape_window_background);
        if (mWindowBackgroundDrawable != null) {
            mWindowBackgroundDrawable = mWindowBackgroundDrawable.mutate();
            window.setBackgroundDrawable(mWindowBackgroundDrawable);
        }
        mDecorBackgroundDrawable =
                getContext().getDrawable(R.drawable.dialog_qs_tile_icon_shape_window_background);
        if (mDecorBackgroundDrawable != null) {
            mDecorBackgroundDrawable = mDecorBackgroundDrawable.mutate();
            View decor = window.getDecorView();
            if (decor != null) {
                decor.setBackground(mDecorBackgroundDrawable);
                decor.setClipToOutline(true);
            }
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setBackgroundBlurRadius(BACKGROUND_BLUR_RADIUS);
        window.setDimAmount(DIM_AMOUNT_WITH_BLUR);
        if (mWindowBackgroundDrawable != null) {
            mWindowBackgroundDrawable.setAlpha(WINDOW_BG_ALPHA_WITH_BLUR);
        }
        if (mDecorBackgroundDrawable != null) {
            mDecorBackgroundDrawable.setAlpha(WINDOW_BG_ALPHA_WITH_BLUR);
        }
        mBlurEnabledListener = this::updateWindowForBlur;
        window.getWindowManager().addCrossWindowBlurEnabledListener(mBlurEnabledListener);
        boolean enabled = window.getWindowManager().isCrossWindowBlurEnabled();
        updateWindowForBlur(enabled);
    }

    private void updateWindowForBlur(boolean blursEnabled) {
        if (mDialog == null) return;
        Window window = mDialog.getWindow();
        if (window == null) return;
        int alpha = blursEnabled && BACKGROUND_BLUR_RADIUS > 0
                ? WINDOW_BG_ALPHA_WITH_BLUR
                : WINDOW_BG_ALPHA_NO_BLUR;
        if (mWindowBackgroundDrawable != null) {
            mWindowBackgroundDrawable.setAlpha(alpha);
        }
        if (mDecorBackgroundDrawable != null) {
            mDecorBackgroundDrawable.setAlpha(alpha);
        }
        window.setDimAmount(blursEnabled && BACKGROUND_BLUR_RADIUS > 0
                ? DIM_AMOUNT_WITH_BLUR
                : DIM_AMOUNT_NO_BLUR);
        window.setBackgroundBlurRadius(BACKGROUND_BLUR_RADIUS);
        window.setAttributes(window.getAttributes());
    }

    private static final class TileIconShapePreviewDrawable extends Drawable {
        private final float mViewBox;
        private final float mStrokeFraction;

        private final Path mPath;
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        TileIconShapePreviewDrawable(String pathData, float viewBox, int color, float strokeFraction) {
            mViewBox = viewBox > 0f ? viewBox : 100f;
            mStrokeFraction = strokeFraction;
            Path path;
            try {
                path = PathParser.createPathFromPathData(pathData);
            } catch (RuntimeException e) {
                path = PathParser.createPathFromPathData(
                        QsTileIconShapePathData.pathStringForKey(
                                QsTileIconShapePathData.DEFAULT_KEY));
            }
            mPath = path;
            mPaint.setColor(color);
            if (strokeFraction > 0f) {
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setStrokeJoin(Paint.Join.ROUND);
                mPaint.setStrokeCap(Paint.Cap.ROUND);
            } else {
                mPaint.setStyle(Paint.Style.FILL);
            }
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
            if (mStrokeFraction > 0f) {
                float minPx = Math.min(b.width(), b.height());
                float strokePath = (minPx * mStrokeFraction) / Math.min(sx, sy);
                mPaint.setStrokeWidth(strokePath);
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

    private static class TileIconShapeAdapter extends android.widget.BaseAdapter {
        private final LayoutInflater mInflater;
        private final String[] mEntries;
        private final String[] mEntryValues;
        private final QsTileIconShapePreference mPreference;
        private final int mSelectedIndex;
        private final Drawable mSelectedBackground;

        TileIconShapeAdapter(Context context, String[] entries, String[] entryValues,
                QsTileIconShapePreference preference, int selectedIndex) {
            mInflater = LayoutInflater.from(context);
            mEntries = entries;
            mEntryValues = entryValues;
            mPreference = preference;
            mSelectedIndex = selectedIndex;
            mSelectedBackground = context.getDrawable(R.drawable.qs_tile_icon_shape_item_selected);
        }

        @Override
        public int getCount() {
            return mEntries.length;
        }

        @Override
        public Object getItem(int position) {
            return mEntries[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.qs_tile_icon_shape_list_item, parent, false);
            }
            convertView.setBackground(position == mSelectedIndex ? mSelectedBackground : null);
            TextView text = convertView.findViewById(android.R.id.text1);
            android.widget.ImageView icon = convertView.findViewById(R.id.qs_tile_icon_shape_preview);
            text.setText(mEntries[position]);
            String key = mEntryValues[position];
            Drawable d = mPreference.createPreviewDrawable(key);
            icon.setImageDrawable(d);
            icon.setVisibility(d != null ? View.VISIBLE : View.GONE);
            return convertView;
        }
    }
}
