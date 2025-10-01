package org.derpfest.customizations.preference;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
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

        TextView title = (TextView) holder.findViewById(R.id.title);
        TextView summary = (TextView) holder.findViewById(R.id.summary);
        ImageButton deleteButton = (ImageButton) holder.findViewById(R.id.delete_button);

        title.setText(getTitle());
        summary.setText(getSummary());

        holder.itemView.setOnClickListener(v -> {
            if (mFilePickerLauncher != null) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("application/json");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                mFilePickerLauncher.launch(intent);
            }
        });

        deleteButton.setOnClickListener(v -> {
            Settings.Secure.putString(getContext().getContentResolver(),
                    Settings.Secure.PIF_DATA, null);
            Toast.makeText(getContext(), "User PIF data cleared", Toast.LENGTH_SHORT).show();
            callChangeListener(null);

            killGms();
        });
    }

    public void handleFileSelected(Uri uri) {
        if (uri == null ||
            (!uri.toString().endsWith(".json") &&
             !"application/json".equals(getContext().getContentResolver().getType(uri)))) {
            Toast.makeText(getContext(), "Invalid file selected", Toast.LENGTH_SHORT).show();
            return;
        }

        try (InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line).append('\n');
            }

            String json = jsonContent.toString();

            Settings.Secure.putString(getContext().getContentResolver(),
                    Settings.Secure.PIF_DATA, json);
            Toast.makeText(getContext(), "JSON file loaded", Toast.LENGTH_SHORT).show();
            callChangeListener(json);

            killGms();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read JSON file", e);
            Toast.makeText(getContext(), "Failed to read JSON", Toast.LENGTH_SHORT).show();
        }
    }

    private void killGms() {
        try {
            ActivityManager am = (ActivityManager)
                    getContext().getSystemService(Context.ACTIVITY_SERVICE);
            am.getClass()
              .getMethod("forceStopPackage", String.class)
              .invoke(am, "com.google.android.gms");
            Log.i(TAG, "GMS process killed");
        } catch (Exception e) {
            Log.e(TAG, "Failed to kill GMS process", e);
        }
    }
}
