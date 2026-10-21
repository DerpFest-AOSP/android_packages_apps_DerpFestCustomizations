package org.derpfest.customizations.preference;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KeyboxDataPreference extends Preference {

    private static final String TAG = "KeyboxDataPref";
    private static final int DOWNLOAD_TIMEOUT_MS = 15_000;
    private static final String KEYBOX_URL = "https://derpfest.org/keybox.xml";
    private static final String REVOCATION_URL =
            "https://android.googleapis.com/attestation/status?encrypted=0";
    private static final Pattern CERT_PATTERN = Pattern.compile(
            "-----BEGIN CERTIFICATE-----([\\s\\S]+?)-----END CERTIFICATE-----");

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private ActivityResultLauncher<Intent> mFilePickerLauncher;

    public KeyboxDataPreference(Context context, AttributeSet attrs) {
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
        if (ctx == null) return;
        final ContentResolver cr = ctx.getContentResolver();

        TextView title = (TextView) holder.findViewById(R.id.title);
        TextView summary = (TextView) holder.findViewById(R.id.summary);
        ImageButton deleteButton = (ImageButton) holder.findViewById(R.id.delete_button);

        title.setText(getTitle());

        String keyboxData = Settings.Secure.getString(cr, Settings.Secure.KEYBOX_DATA);
        String keyboxTimestamp = Settings.Secure.getString(cr, Settings.Secure.KEYBOX_DATA_TIMESTAMP);
        boolean hasData = keyboxData != null;

        if (hasData) {
            KeyboxInfo info = parseKeyboxInfo(keyboxData);
            String ts = keyboxTimestamp != null ? keyboxTimestamp :
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
            summary.setText(ctx.getString(
                R.string.keybox_data_loaded_summary,
                info.type,
                info.certCount,
                ts
            ));
        } else {
            summary.setText(ctx.getString(R.string.keybox_data_summary));
        }

        deleteButton.setVisibility(hasData ? View.VISIBLE : View.GONE);
        deleteButton.setEnabled(hasData);

        holder.itemView.setOnClickListener(v -> {
            if (mFilePickerLauncher != null) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/xml", "application/xml"});
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                mFilePickerLauncher.launch(intent);
            }
        });

        deleteButton.setOnClickListener(v -> {
            Settings.Secure.putString(cr, Settings.Secure.KEYBOX_DATA, null);
            Settings.Secure.putString(cr, Settings.Secure.KEYBOX_DATA_TIMESTAMP, null);
            Toast.makeText(ctx, ctx.getString(R.string.keybox_toast_file_cleared), Toast.LENGTH_SHORT).show();
            notifyChanged();
        });
    }


    public void handleFileSelected(Uri uri) {
        final Context ctx = getContext();
        if (ctx == null) return;
        final ContentResolver cr = ctx.getContentResolver();

        if (uri == null) {
            Toast.makeText(ctx,
                ctx.getString(R.string.keybox_toast_invalid_file_selected), Toast.LENGTH_SHORT).show();
            return;
        }

        final String type = cr.getType(uri);
        boolean isXmlMime = "text/xml".equals(type) || "application/xml".equals(type);
        boolean hasXmlExt = (uri.getPath() != null && uri.getPath().toLowerCase().endsWith(".xml"));
        if (!isXmlMime && !hasXmlExt) {
            Toast.makeText(ctx,
                ctx.getString(R.string.keybox_toast_invalid_file_selected), Toast.LENGTH_SHORT).show();
            return;
        }

        try (InputStream inputStream = cr.openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                 new InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8))) {
            StringBuilder xmlContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                xmlContent.append(line).append('\n');
            }

            String xml = xmlContent.toString();
            if (!storeXml(xml)) {
                Toast.makeText(ctx,
                    ctx.getString(R.string.keybox_toast_missing_data), Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(ctx,
                    ctx.getString(R.string.keybox_toast_file_loaded), Toast.LENGTH_SHORT).show();
            restartAttestationClients();
        } catch (IOException e) {
            Log.e(TAG, "Failed to read XML file", e);
            Toast.makeText(ctx,
                ctx.getString(R.string.keybox_toast_invalid_file_selected), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Validates and stores a keybox XML in the setting the attestation path reads.
     *
     * @return true when the XML was stored
     */
    public boolean storeXml(String xml) {
        final Context ctx = getContext();
        if (ctx == null || xml == null || !validateXml(xml)) {
            return false;
        }
        ContentResolver cr = ctx.getContentResolver();
        Settings.Secure.putString(cr, Settings.Secure.KEYBOX_DATA, xml);
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(new Date());
        Settings.Secure.putString(cr, Settings.Secure.KEYBOX_DATA_TIMESTAMP, timestamp);
        notifyChanged();
        return true;
    }

    public void downloadLatest() {
        final Context ctx = getContext();
        if (ctx == null) return;
        setEnabled(false);
        new Thread(() -> {
            String xml = fetchUrl(KEYBOX_URL);
            Boolean revoked = xml != null ? isRevoked(xml) : null;
            mHandler.post(() -> {
                setEnabled(true);
                if (xml == null) {
                    toast(ctx, R.string.keybox_toast_download_failed);
                    return;
                }
                if (!storeXml(xml)) {
                    toast(ctx, R.string.keybox_toast_missing_data);
                    return;
                }
                toast(ctx, R.string.keybox_toast_file_loaded);
                if (revoked != null) {
                    toast(ctx, revoked
                            ? R.string.keybox_toast_download_revoked
                            : R.string.keybox_toast_download_valid);
                }
                restartAttestationClients();
            });
        }, "KeyboxDownload").start();
    }

    private static String fetchUrl(String url) {
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
                    connection.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line).append('\n');
                }
                return body.toString();
            }
        } catch (Exception e) {
            Log.e(TAG, "Keybox download failed", e);
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    /** @return true if revoked, false if not, null if the status list could not be read */
    private static Boolean isRevoked(String xml) {
        String status = fetchUrl(REVOCATION_URL);
        if (status == null) return null;
        try {
            org.json.JSONObject entries = new org.json.JSONObject(status).optJSONObject("entries");
            if (entries == null) return null;
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            Matcher matcher = CERT_PATTERN.matcher(xml);
            boolean sawCert = false;
            while (matcher.find()) {
                byte[] der = Base64.decode(matcher.group(1).replaceAll("\\s", ""), Base64.DEFAULT);
                X509Certificate cert = (X509Certificate) factory.generateCertificate(
                        new ByteArrayInputStream(der));
                sawCert = true;
                org.json.JSONObject entry = entries.optJSONObject(
                        cert.getSerialNumber().toString(16).toUpperCase(Locale.US));
                if (entry == null) continue;
                String state = entry.optString("status", "").toUpperCase(Locale.US);
                if ("REVOKED".equals(state) || "SUSPENDED".equals(state)) {
                    return true;
                }
            }
            return sawCert ? false : null;
        } catch (Exception e) {
            Log.w(TAG, "Keybox revocation check failed", e);
            return null;
        }
    }

    private void restartAttestationClients() {
        Context ctx = getContext();
        if (ctx == null) return;
        ActivityManager am = ctx.getSystemService(ActivityManager.class);
        if (am == null) return;
        try {
            am.forceStopPackage("com.google.android.gms");
            am.forceStopPackage("com.android.vending");
            am.forceStopPackage("com.google.android.rkpdapp");
        } catch (Exception e) {
            Log.w(TAG, "Failed to restart attestation clients", e);
        }
    }

    private static void toast(Context ctx, int resId) {
        Toast.makeText(ctx, ctx.getString(resId), Toast.LENGTH_SHORT).show();
    }

    private static final class KeyboxInfo {
        final String type;
        final int certCount;
        final String timestamp;

        KeyboxInfo(String type, int certCount, String timestamp) {
            this.type = type;
            this.certCount = certCount;
            this.timestamp = timestamp;
        }
    }

    private KeyboxInfo parseKeyboxInfo(String xml) {
        boolean hasEcdsaKey = false;
        boolean hasRsaKey = false;
        int certCount = 0;

        try {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(new StringReader(xml));

            String currentAlg = null;
            for (int eventType = parser.next(); eventType != XmlPullParser.END_DOCUMENT; eventType = parser.next()) {
                if (eventType == XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    if ("Key".equals(name)) {
                        currentAlg = parser.getAttributeValue(null, "algorithm");
                        if ("ecdsa".equalsIgnoreCase(currentAlg)) {
                            hasEcdsaKey = true;
                        } else if ("rsa".equalsIgnoreCase(currentAlg)) {
                            hasRsaKey = true;
                        }
                    } else if ("Certificate".equals(name)) {
                        certCount++;
                    }
                } else if (eventType == XmlPullParser.END_TAG && "Key".equals(parser.getName())) {
                    currentAlg = null;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse keybox info", e);
        }

        String type;
        if (hasEcdsaKey && hasRsaKey) {
            type = "RSA + ECDSA";
        } else if (hasEcdsaKey) {
            type = "ECDSA";
        } else if (hasRsaKey) {
            type = "RSA";
        } else {
            type = "Unknown";
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        return new KeyboxInfo(type, certCount, timestamp);
    }

    private boolean validateXml(String xml) {
        boolean hasEcdsaKey = false, hasRsaKey = false;
        boolean hasEcdsaPrivKey = false, hasRsaPrivKey = false;
        int ecdsaCertCount = 0, rsaCertCount = 0;
        int numberOfKeyboxes = -1;

        try {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(new StringReader(xml));

            String currentAlg = null;

            for (int eventType = parser.next(); eventType != XmlPullParser.END_DOCUMENT; eventType = parser.next()) {
                if (eventType == XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    switch (name) {
                        case "NumberOfKeyboxes":
                            parser.next(); // move to TEXT event
                            if (parser.getEventType() == XmlPullParser.TEXT) {
                                try {
                                    numberOfKeyboxes = Integer.parseInt(parser.getText().trim());
                                } catch (NumberFormatException e) {
                                    numberOfKeyboxes = -1;
                                }
                            }
                            break;

                        case "Key":
                            currentAlg = parser.getAttributeValue(null, "algorithm");
                            if ("ecdsa".equalsIgnoreCase(currentAlg)) {
                                hasEcdsaKey = true;
                            } else if ("rsa".equalsIgnoreCase(currentAlg)) {
                                hasRsaKey = true;
                            } else {
                                currentAlg = null; // unsupported key
                            }
                            break;

                        case "PrivateKey": {
                            String format = parser.getAttributeValue(null, "format");
                            if (!"pem".equalsIgnoreCase(format)) {
                                Log.w(TAG, "Invalid or missing format for PrivateKey");
                                return false;
                            }
                            if ("ecdsa".equalsIgnoreCase(currentAlg)) {
                                hasEcdsaPrivKey = true;
                            } else if ("rsa".equalsIgnoreCase(currentAlg)) {
                                hasRsaPrivKey = true;
                            }
                            break;
                        }

                        case "Certificate": {
                            String format = parser.getAttributeValue(null, "format");
                            if (!"pem".equalsIgnoreCase(format)) {
                                Log.w(TAG, "Invalid or missing format for Certificate");
                                return false;
                            }

                            if ("ecdsa".equalsIgnoreCase(currentAlg)) {
                                ecdsaCertCount++;
                            } else if ("rsa".equalsIgnoreCase(currentAlg)) {
                                rsaCertCount++;
                            }
                            break;
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG && "Key".equals(parser.getName())) {
                    currentAlg = null;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "XML validation failed", e);
            return false;
        }

        return numberOfKeyboxes == 1
                && hasEcdsaKey && hasEcdsaPrivKey && ecdsaCertCount >= 1
                && hasRsaKey && hasRsaPrivKey && rsaCertCount >= 1;
    }
}
