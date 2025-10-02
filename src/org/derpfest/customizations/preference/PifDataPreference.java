package org.derpfest.customizations.preference;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

public class PifDataPreference extends Preference {

    private static final String TAG = "PifDataPref";
    private ActivityResultLauncher<Intent> mFilePickerLauncher;

    public PifDataPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.pref_with_delete);
    }

    public void setFilePickerLauncher(ActivityResultLauncher<Intent> launcher) {
        this.mFilePickerLauncher = launcher;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final Context ctx = getContext();
        final ContentResolver cr = ctx.getContentResolver();

        TextView title = (TextView) holder.findViewById(R.id.title);
        TextView summary = (TextView) holder.findViewById(R.id.summary);
        ImageButton deleteButton = (ImageButton) holder.findViewById(R.id.delete_button);

        title.setText(getTitle());

        boolean hasData = Settings.Secure.getString(
                cr, Settings.Secure.PIF_DATA) != null;

        summary.setText(ctx.getString(
                hasData ? R.string.pif_data_loaded_summary : R.string.pif_data_summary));

        deleteButton.setVisibility(hasData ? View.VISIBLE : View.GONE);
        deleteButton.setEnabled(hasData);

        holder.itemView.setOnClickListener(v -> {
            if (mFilePickerLauncher != null) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/json"});
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                mFilePickerLauncher.launch(intent);
            }
        });

        deleteButton.setOnClickListener(v -> {
            if (!callChangeListener(Boolean.FALSE)) return;
            Settings.Secure.putString(cr, Settings.Secure.PIF_DATA, null);
            Toast.makeText(ctx, ctx.getString(R.string.pif_toast_file_cleared), Toast.LENGTH_SHORT).show();
            notifyChanged();
            killPackages();
        });
    }

    public void handleFileSelected(Uri uri) {
        final Context ctx = getContext();
        final ContentResolver cr = ctx.getContentResolver();

        if (uri == null) {
            Toast.makeText(ctx,
                ctx.getString(R.string.pif_toast_invalid_file_selected), Toast.LENGTH_SHORT).show();
            return;
        }

        final String type = cr.getType(uri);
        boolean isJsonMime = "application/json".equals(type) || "text/json".equals(type);
        boolean hasJsonExt = (uri.getPath() != null && uri.getPath().toLowerCase().endsWith(".json"));
        if (!isJsonMime && !hasJsonExt) {
            Toast.makeText(ctx,
                ctx.getString(R.string.pif_toast_invalid_file_selected), Toast.LENGTH_SHORT).show();
            return;
        }

        try (InputStream inputStream = cr.openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                 new InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8))) {

            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line).append('\n');
            }

            String json = jsonContent.toString();

            if (!callChangeListener(Boolean.TRUE)) return;
            Settings.Secure.putString(cr, Settings.Secure.PIF_DATA, json);
            Toast.makeText(ctx,
                    ctx.getString(R.string.pif_toast_file_loaded), Toast.LENGTH_SHORT).show();
            notifyChanged();
            killPackages();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read JSON file", e);
            Toast.makeText(ctx,
                ctx.getString(R.string.pif_toast_invalid_file_selected), Toast.LENGTH_SHORT).show();
        }
    }

    private void killPackages() {
        try {
            ActivityManager am = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
            String[] packages = { "com.google.android.gms", "com.android.vending" };
            for (String pkg : packages) {
                am.getClass()
                  .getMethod("forceStopPackage", String.class)
                  .invoke(am, pkg);
                Log.i(TAG, pkg + " process killed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to kill packages", e);
        }
    }
}
