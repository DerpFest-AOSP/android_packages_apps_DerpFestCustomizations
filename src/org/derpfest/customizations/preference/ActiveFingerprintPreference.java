package org.derpfest.customizations.preference;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * Shows the fingerprint {@code PropImitationHooks} will apply:
 * an imported file, otherwise the fetched profile, otherwise the built-in one.
 */
public class ActiveFingerprintPreference extends Preference {

    private static final String TAG = "ActiveFingerprintPref";
    private static final int DOWNLOAD_TIMEOUT_MS = 10_000;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private int mSourceLabel;

    public ActiveFingerprintPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSelectable(false);
        setPersistent(false);
        setSingleLineTitle(false);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        refresh();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View iconFrame = holder.findViewById(
                com.android.settingslib.widget.theme.R.id.icon_frame);
        if (iconFrame != null) {
            iconFrame.setVisibility(View.GONE);
        }
        TextView title = (TextView) holder.findViewById(android.R.id.title);
        if (title != null) {
            title.setSingleLine(false);
            title.setMaxLines(2);
        }
        TextView summary = (TextView) holder.findViewById(android.R.id.summary);
        if (summary != null) {
            summary.setSingleLine(false);
            summary.setMaxLines(4);
            summary.setEllipsize(TextUtils.TruncateAt.END);
        }
        bindSource(holder);
    }

    private void bindSource(PreferenceViewHolder holder) {
        View footer = holder.findViewById(
                com.android.settingslib.widget.theme.R.id.settingslib_expressive_footer_container);
        TextView footerText = (TextView) holder.findViewById(
                com.android.settingslib.widget.theme.R.id.settingslib_expressive_footer_text);
        if (footer == null || footerText == null || mSourceLabel == 0) {
            return;
        }
        Context ctx = getContext();
        if (ctx == null) return;
        int gap = (int) (8 * ctx.getResources().getDisplayMetrics().density);
        footer.setPaddingRelative(footer.getPaddingStart(), gap,
                footer.getPaddingEnd(), footer.getPaddingBottom());
        footer.setVisibility(View.VISIBLE);
        footerText.setVisibility(View.VISIBLE);
        footerText.setClickable(false);
        footerText.setFocusable(false);
        footerText.setText(ctx.getString(mSourceLabel));
    }

    public void refresh() {
        Context ctx = getContext();
        if (ctx == null) return;
        Profile profile = read(ctx);
        mSourceLabel = profile.sourceLabel;
        setTitle(TextUtils.isEmpty(profile.model)
                ? ctx.getString(R.string.pif_active_title)
                : profile.model);
        setSummary(TextUtils.isEmpty(profile.fingerprint)
                ? ctx.getString(R.string.pif_active_none)
                : profile.fingerprint);
        notifyChanged();
    }

    /**
     * Downloads the certified profile into the fetched setting. An imported file stays in use.
     */
    public void downloadLatest(Runnable onFinished) {
        final Context appContext = getContext() == null
                ? null : getContext().getApplicationContext();
        if (appContext == null) {
            if (onFinished != null) onFinished.run();
            return;
        }
        final String url = appContext.getString(com.android.internal.R.string.config_pifUpdateUrl);
        new Thread(() -> {
            String body = fetch(url);
            boolean valid = isValidJson(body);
            mHandler.post(() -> {
                applyDownload(appContext, body, valid);
                if (onFinished != null) onFinished.run();
            });
        }, "PifRefresh").start();
    }

    private void applyDownload(Context ctx, String body, boolean valid) {
        if (!valid) {
            toast(ctx, R.string.pif_toast_refresh_failed);
            return;
        }
        ContentResolver cr = ctx.getContentResolver();
        String saved = Settings.Secure.getString(cr, Settings.Secure.FETCHED_PIF);
        boolean changed = !TextUtils.equals(saved, body);
        if (changed) {
            Settings.Secure.putString(cr, Settings.Secure.FETCHED_PIF, body);
        }
        boolean imported = !TextUtils.isEmpty(
                Settings.Secure.getString(cr, Settings.Secure.PIF_DATA));
        refresh();
        if (!changed) {
            toast(ctx, R.string.pif_toast_refresh_unchanged);
            return;
        }
        if (imported) {
            toast(ctx, R.string.pif_toast_refresh_imported);
            return;
        }
        toast(ctx, R.string.pif_toast_refresh_updated);
        restartIntegrityClients(ctx);
    }

    private static Profile read(Context ctx) {
        ContentResolver cr = ctx.getContentResolver();
        String imported = Settings.Secure.getString(cr, Settings.Secure.PIF_DATA);
        if (!TextUtils.isEmpty(imported)) {
            Profile profile = fromJson(imported, R.string.pif_active_source_imported);
            if (profile != null) return profile;
        } else {
            String fetched = Settings.Secure.getString(cr, Settings.Secure.FETCHED_PIF);
            if (!TextUtils.isEmpty(fetched)) {
                Profile profile = fromJson(fetched, R.string.pif_active_source_fetched);
                if (profile != null) return profile;
            }
        }
        return fromBuiltin(ctx);
    }

    private static Profile fromJson(String json, int sourceLabel) {
        try {
            Profile profile = new Profile(sourceLabel);
            JSONObject parsed = new JSONObject(json);
            Iterator<String> keys = parsed.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object raw = parsed.opt(key);
                if (raw == null || raw == JSONObject.NULL
                        || raw instanceof JSONObject || raw instanceof JSONArray) {
                    continue;
                }
                applyField(profile, key, String.valueOf(raw));
            }
            return profile;
        } catch (JSONException e) {
            return null;
        }
    }

    private static Profile fromBuiltin(Context ctx) {
        Profile profile = new Profile(R.string.pif_active_source_builtin);
        for (String entry : ctx.getResources().getStringArray(
                com.android.internal.R.array.config_certifiedBuildProperties)) {
            String[] parts = entry.split(":", 2);
            if (parts.length == 2) {
                applyField(profile, parts[0], parts[1]);
            }
        }
        return profile;
    }

    private static void applyField(Profile profile, String key, String value) {
        if ("MODEL".equals(key)) {
            profile.model = value;
        } else if ("FINGERPRINT".equals(key)) {
            profile.fingerprint = value;
        }
    }

    private static String fetch(String url) {
        if (TextUtils.isEmpty(url)) return null;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(DOWNLOAD_TIMEOUT_MS);
            connection.setReadTimeout(DOWNLOAD_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
                return body.toString();
            }
        } catch (Exception e) {
            Log.e(TAG, "Fingerprint download failed", e);
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static boolean isValidJson(String body) {
        if (TextUtils.isEmpty(body)) return false;
        try {
            return new JSONObject(body).length() > 0;
        } catch (JSONException e) {
            return false;
        }
    }

    private static void restartIntegrityClients(Context ctx) {
        ActivityManager am = ctx.getSystemService(ActivityManager.class);
        if (am == null) return;
        try {
            am.forceStopPackage("com.google.android.gms");
            am.forceStopPackage("com.android.vending");
        } catch (Exception e) {
            Log.w(TAG, "Failed to restart integrity clients", e);
        }
    }

    private static void toast(Context ctx, int resId) {
        Toast.makeText(ctx, ctx.getString(resId), Toast.LENGTH_SHORT).show();
    }

    private static final class Profile {
        final int sourceLabel;
        String model;
        String fingerprint;

        Profile(int sourceLabel) {
            this.sourceLabel = sourceLabel;
        }
    }
}
