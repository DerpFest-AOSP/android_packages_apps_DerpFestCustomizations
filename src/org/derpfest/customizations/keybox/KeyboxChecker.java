/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.keybox;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import com.android.settings.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates the keybox attestation currently in use.
 *
 * <p>The checks match Evolution X Evolver's TrickyStore revocation check: each
 * certificate chain is ordered leaf to root and verified link by link, the root
 * must be a Google attestation root, the earliest leaf must still be inside its
 * validity window, and every serial is compared with Google's revocation list.
 * A response that cannot be read, including a JSON body with no entries
 * object, keeps a known bad serial and never reports the keybox as valid.
 */
public final class KeyboxChecker {

    private static final String TAG = "KeyboxChecker";
    private static final String PREFS = "keybox_status";

    private static final String ROOTS_URL = "https://android.googleapis.com/attestation/root";
    private static final String REVOCATION_URL =
            "https://android.googleapis.com/attestation/status?encrypted=0";

    /** Warn, via the chip color, when the leaf expires within this window. */
    private static final long EXPIRY_WARN_MS = TimeUnit.DAYS.toMillis(14);
    private static final long ROOTS_CACHE_TTL_MS = TimeUnit.DAYS.toMillis(30);
    private static final long CACHE_TRUST_WINDOW_MS = TimeUnit.DAYS.toMillis(7);
    private static final int TIMEOUT_MS = 10_000;
    private static final long DAY_MS = TimeUnit.DAYS.toMillis(1);

    // Fallback trust anchors, used only when the live root fetch and the cache
    // are both unavailable. SHA-256 of the DER-encoded certificate.
    private static final String FALLBACK_ROOT_RSA_2019_SHA256 =
            "1EF1A04B8BA58AB94589AC498C8982A783F24EA7307E0159A0C3A73B377D87CC";
    private static final String FALLBACK_ROOT_RSA_2022_SHA256 =
            "CEDB1CB6DC896AE5EC797348BCE9286753C2B38EE71CE0FBE34A9A1248800DFC";
    private static final String FALLBACK_ROOT_EC_2025_SHA256 =
            "6D9DB4CE6C5C0B293166D08986E05774A8776CEB525D9E4329520DE12BA4BCC0";

    private static final String KEY_ROOTS = "roots";
    private static final String KEY_ROOTS_AT = "roots_at";
    private static final String KEY_REVOKED_SERIAL = "revoked_serial";
    private static final String KEY_REVOKED_STATUS = "revoked_status";
    private static final String KEY_REVOKED_AT = "revoked_at";

    private static final Pattern KEY_BLOCK = Pattern.compile(
            "<Key\\b[^>]*>([\\s\\S]*?)</Key>", Pattern.CASE_INSENSITIVE);
    private static final Pattern CERT_BLOCK = Pattern.compile(
            "-----BEGIN CERTIFICATE-----([\\s\\S]+?)-----END CERTIFICATE-----");

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "KeyboxChecker");
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        return thread;
    });
    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final CopyOnWriteArrayList<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private static volatile Result sCached;

    public interface Listener {
        void onResult(Result result);
    }

    public interface Callback {
        void onResult(Result result);
    }

    public enum Status {
        CHECKING,
        NONE,
        VALID,
        EXPIRING_SOON,
        REVOKED,
        SUSPENDED,
        EXPIRED,
        CHAIN_INVALID,
        UNTRUSTED_ROOT,
        UNKNOWN
    }

    public enum Tone {
        POSITIVE,
        WARNING,
        NEGATIVE,
        NEUTRAL
    }

    public static final class Result {
        public final Status status;
        public final String detail;
        public final long expiresAtMillis;
        public final String sourceId;

        private Result(Status status, String detail, long expiresAtMillis, String sourceId) {
            this.status = status;
            this.detail = detail == null ? "" : detail;
            this.expiresAtMillis = expiresAtMillis;
            this.sourceId = sourceId == null ? "" : sourceId;
        }

        /** Applies certificate expiry that has passed since the check ran. */
        public Result at(long now) {
            if (expiresAtMillis > 0 && expiresAtMillis <= now
                    && (status == Status.VALID || status == Status.EXPIRING_SOON
                    || status == Status.UNKNOWN)) {
                return new Result(Status.EXPIRED, "CERT_EXPIRED", expiresAtMillis, sourceId);
            }
            return this;
        }

        public int daysRemaining(long now) {
            if (expiresAtMillis <= now) return 0;
            return (int) ((expiresAtMillis - now) / DAY_MS);
        }

        public boolean notifies() {
            switch (status) {
                case REVOKED:
                case SUSPENDED:
                case EXPIRED:
                case CHAIN_INVALID:
                case UNTRUSTED_ROOT:
                    return true;
                default:
                    return false;
            }
        }

        public Tone tone(long now) {
            Result current = at(now);
            switch (current.status) {
                case VALID:
                    return Tone.POSITIVE;
                case EXPIRING_SOON:
                case SUSPENDED:
                case UNTRUSTED_ROOT:
                    return Tone.WARNING;
                case REVOKED:
                case EXPIRED:
                case CHAIN_INVALID:
                    return Tone.NEGATIVE;
                case UNKNOWN:
                default:
                    return Tone.NEUTRAL;
            }
        }

        /**
         * Chip text. A keybox that passed the same checks Evolver uses for "valid"
         * shows the days left on the earliest leaf. Other results name the failure.
         * Returns null when there is no keybox to describe.
         */
        public String chipLabel(Context context, long now) {
            Result current = at(now);
            switch (current.status) {
                case NONE:
                    return null;
                case CHECKING:
                    return context.getString(R.string.keybox_status_checking);
                case VALID:
                case EXPIRING_SOON:
                    if (current.expiresAtMillis > 0) {
                        int days = current.daysRemaining(now);
                        if (days <= 0) {
                            return context.getString(R.string.keybox_status_today);
                        }
                        return context.getResources().getQuantityString(
                                R.plurals.keybox_status_days, days, days);
                    }
                    return context.getString(R.string.keybox_status_unchecked);
                case REVOKED:
                    return context.getString(R.string.keybox_status_revoked);
                case SUSPENDED:
                    return context.getString(R.string.keybox_status_suspended);
                case EXPIRED:
                    return context.getString(R.string.keybox_status_expired);
                case CHAIN_INVALID:
                    return context.getString(R.string.keybox_status_invalid);
                case UNTRUSTED_ROOT:
                    return context.getString(R.string.keybox_status_untrusted);
                default:
                    return context.getString(R.string.keybox_status_unchecked);
            }
        }
    }

    private KeyboxChecker() {}

    public static void addListener(Listener listener) {
        LISTENERS.remove(listener);
        LISTENERS.add(listener);
    }

    public static void removeListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    public static Result cached() {
        return sCached;
    }

    public static Result checking() {
        return new Result(Status.CHECKING, "", 0L, "");
    }

    /** Reads the active keybox and runs the full check. Call off the main thread. */
    public static Result checkActive(Context context) {
        try {
            String xml = Settings.Secure.getString(
                    context.getContentResolver(), Settings.Secure.KEYBOX_DATA);
            if (xml != null && !xml.trim().isEmpty()) {
                return checkXml(context, xml);
            }
            List<List<X509Certificate>> chains = chainsFromConfig(context);
            if (chains.isEmpty()) {
                return new Result(Status.NONE, "", 0L, "none");
            }
            return evaluate(context, chains, sourceId(joinChains(chains)));
        } catch (Exception e) {
            Log.e(TAG, "Keybox check failed", e);
            return new Result(Status.UNKNOWN, "", 0L, "");
        }
    }

    /** Runs the full check against a keybox XML. Call off the main thread. */
    public static Result checkXml(Context context, String xml) {
        try {
            if (xml == null || xml.trim().isEmpty()) {
                return new Result(Status.NONE, "", 0L, "none");
            }
            String clean = sanitize(xml);
            List<List<X509Certificate>> chains = extractChains(clean);
            if (chains.isEmpty()) {
                return new Result(Status.UNKNOWN, "", 0L, sourceId(clean));
            }
            return evaluate(context, chains, sourceId(clean));
        } catch (Exception e) {
            Log.e(TAG, "Keybox XML check failed", e);
            return new Result(Status.UNKNOWN, "", 0L, "");
        }
    }

    /**
     * Checks the active keybox and publishes the result to listeners.
     * {@code callback} runs on the main thread even when a newer check superseded this one,
     * in which case the result is null.
     */
    public static void refreshActive(Context context, Callback callback) {
        Context app = context.getApplicationContext();
        final int sequence = SEQUENCE.incrementAndGet();
        EXECUTOR.execute(() -> publish(app, checkActive(app), callback, sequence));
    }

    /** Drops any in-flight check and publishes {@code result} as the current one. */
    public static void supersede(Context context, Result result) {
        int sequence = SEQUENCE.incrementAndGet();
        publish(context.getApplicationContext(), result, null, sequence);
    }

    private static void publish(Context app, Result result, Callback callback, int sequence) {
        MAIN.post(() -> {
            if (sequence != SEQUENCE.get()) {
                if (callback != null) callback.onResult(null);
                return;
            }
            sCached = result;
            KeyboxStatusNotifier.apply(app, result);
            for (Listener listener : LISTENERS) {
                listener.onResult(result);
            }
            if (callback != null) callback.onResult(result);
        });
    }

    private static Result evaluate(Context context, List<List<X509Certificate>> chains,
            String sourceId) {
        Set<X509Certificate> anchors = getTrustAnchors(context);
        String chainError = null;
        for (List<X509Certificate> chain : chains) {
            chainError = validateChain(chain, anchors);
            if (chainError != null) break;
        }
        if (chainError != null) {
            Status status = "UNTRUSTED_ROOT".equals(chainError)
                    ? Status.UNTRUSTED_ROOT : Status.CHAIN_INVALID;
            return new Result(status, chainError, 0L, sourceId);
        }

        long leafExpiry = Long.MAX_VALUE;
        for (List<X509Certificate> chain : chains) {
            leafExpiry = Math.min(leafExpiry, chain.get(0).getNotAfter().getTime());
        }
        long now = System.currentTimeMillis();
        if (leafExpiry < now) {
            return new Result(Status.EXPIRED, "CERT_EXPIRED", leafExpiry, sourceId);
        }
        boolean expiringSoon = leafExpiry - now < EXPIRY_WARN_MS;

        List<X509Certificate> flat = new ArrayList<>();
        for (List<X509Certificate> chain : chains) flat.addAll(chain);
        List<String> serials = serialKeys(flat);

        JSONObject revocation = fetchRevocationJson();
        JSONObject entries = revocation == null ? null : revocation.optJSONObject("entries");
        if (entries == null) {
            if (revocation != null) {
                Log.w(TAG, "Revocation response has no entries object");
            }
            Result cached = checkCachedRevocation(context, serials, leafExpiry, sourceId);
            if (cached != null) return cached;
            return new Result(Status.UNKNOWN, "", leafExpiry, sourceId);
        }

        for (String serial : serials) {
            JSONObject entry = entries.optJSONObject(serial);
            if (entry == null) continue;
            String state = entry.optString("status", "").toUpperCase(Locale.US);
            String reason = entry.optString("reason", "").toUpperCase(Locale.US);
            if ("REVOKED".equals(state) || "SUSPENDED".equals(state)) {
                cacheRevokedSerial(context, serial, state);
                Status status = "REVOKED".equals(state) ? Status.REVOKED : Status.SUSPENDED;
                return new Result(status, reason, leafExpiry, sourceId);
            }
        }
        clearCachedRevocation(context);

        if (expiringSoon) {
            return new Result(Status.EXPIRING_SOON, "", leafExpiry, sourceId);
        }
        return new Result(Status.VALID, "", leafExpiry, sourceId);
    }

    private static List<List<X509Certificate>> extractChains(String xml) {
        Matcher matcher = KEY_BLOCK.matcher(xml);
        List<List<X509Certificate>> chains = new ArrayList<>();
        while (matcher.find()) {
            List<X509Certificate> chain = orderChain(extractCertificates(matcher.group(1)));
            if (!chain.isEmpty()) chains.add(chain);
        }
        if (!chains.isEmpty()) return chains;
        List<X509Certificate> single = orderChain(extractCertificates(xml));
        if (single.isEmpty()) return Collections.emptyList();
        return Collections.singletonList(single);
    }

    private static List<List<X509Certificate>> chainsFromConfig(Context context) {
        String[] entries;
        try {
            entries = context.getResources().getStringArray(
                    com.android.internal.R.array.config_certifiedKeybox);
        } catch (Exception e) {
            Log.w(TAG, "Built-in keybox is not available", e);
            return Collections.emptyList();
        }
        Map<String, List<String>> byAlgorithm = new HashMap<>();
        for (String entry : entries) {
            if (entry == null) continue;
            int colon = entry.indexOf(':');
            if (colon <= 0 || colon >= entry.length() - 1) continue;
            String name = entry.substring(0, colon);
            int marker = name.indexOf(".CERT_");
            if (marker <= 0) continue;
            String algorithm = name.substring(0, marker);
            String pem = entry.substring(colon + 1).replace("\\n", "\n");
            byAlgorithm.computeIfAbsent(algorithm, key -> new ArrayList<>()).add(pem);
        }
        List<List<X509Certificate>> chains = new ArrayList<>();
        for (List<String> pems : byAlgorithm.values()) {
            StringBuilder block = new StringBuilder();
            for (String pem : pems) {
                block.append(pem).append('\n');
            }
            List<X509Certificate> chain = orderChain(extractCertificates(block.toString()));
            if (!chain.isEmpty()) chains.add(chain);
        }
        return chains;
    }

    /**
     * Leaf-to-root order, dropping exact duplicates. The leaf is the only certificate
     * that issued none of the others. Anything that is not a single path is returned
     * unchanged so {@link #validateChain} can report it.
     */
    private static List<X509Certificate> orderChain(List<X509Certificate> certs) {
        List<X509Certificate> unique = new ArrayList<>();
        for (X509Certificate cert : certs) {
            if (!unique.contains(cert)) unique.add(cert);
        }
        if (unique.size() < 2) return unique;

        List<X509Certificate> leaves = new ArrayList<>();
        for (X509Certificate cert : unique) {
            boolean issuedAnother = false;
            for (X509Certificate other : unique) {
                if (other != cert && other.getIssuerX500Principal()
                        .equals(cert.getSubjectX500Principal())) {
                    issuedAnother = true;
                    break;
                }
            }
            if (!issuedAnother) leaves.add(cert);
        }
        if (leaves.size() != 1) return unique;

        List<X509Certificate> ordered = new ArrayList<>();
        ordered.add(leaves.get(0));
        while (true) {
            X509Certificate current = ordered.get(ordered.size() - 1);
            X509Certificate next = null;
            for (X509Certificate candidate : unique) {
                if (ordered.contains(candidate)) continue;
                if (!candidate.getSubjectX500Principal()
                        .equals(current.getIssuerX500Principal())) {
                    continue;
                }
                try {
                    current.verify(candidate.getPublicKey());
                    next = candidate;
                    break;
                } catch (Exception ignored) {
                    // Not this issuer.
                }
            }
            if (next == null) break;
            ordered.add(next);
        }
        return ordered.size() == unique.size() ? ordered : unique;
    }

    private static List<X509Certificate> extractCertificates(String xml) {
        List<X509Certificate> certs = new ArrayList<>();
        CertificateFactory factory;
        try {
            factory = CertificateFactory.getInstance("X.509");
        } catch (Exception e) {
            Log.w(TAG, "Certificate factory is unavailable", e);
            return certs;
        }
        Matcher matcher = CERT_BLOCK.matcher(xml);
        while (matcher.find()) {
            try {
                byte[] der = Base64.decode(
                        matcher.group(1).replaceAll("\\s", ""), Base64.DEFAULT);
                certs.add((X509Certificate) factory.generateCertificate(
                        new ByteArrayInputStream(der)));
            } catch (Exception ignored) {
                // Skip a block that is not a certificate. The chain check reports it.
            }
        }
        return certs;
    }

    /**
     * @return null when the chain is valid, otherwise a short error code
     */
    private static String validateChain(List<X509Certificate> certs, Set<X509Certificate> anchors) {
        if (certs.size() < 2) return "CHAIN_TOO_SHORT";

        for (int i = 0; i < certs.size() - 1; i++) {
            try {
                certs.get(i).verify(certs.get(i + 1).getPublicKey());
            } catch (Exception e) {
                return "BAD_SIGNATURE_AT_" + i;
            }
        }

        X509Certificate root = certs.get(certs.size() - 1);
        try {
            root.verify(root.getPublicKey());
        } catch (Exception e) {
            return "ROOT_NOT_SELF_SIGNED";
        }

        // Live anchors are compared by public key, not certificate hash. Google
        // reissues a root under a new certificate with the same key.
        String rootKey = sha256Hex(root.getPublicKey().getEncoded());
        String fingerprint;
        try {
            fingerprint = sha256Hex(root.getEncoded());
        } catch (Exception e) {
            Log.w(TAG, "Attestation root could not be encoded", e);
            fingerprint = "";
        }
        boolean trusted;
        if (!anchors.isEmpty()) {
            trusted = false;
            for (X509Certificate anchor : anchors) {
                if (rootKey.equals(sha256Hex(anchor.getPublicKey().getEncoded()))) {
                    trusted = true;
                    break;
                }
            }
        } else {
            trusted = FALLBACK_ROOT_RSA_2019_SHA256.equals(fingerprint)
                    || FALLBACK_ROOT_RSA_2022_SHA256.equals(fingerprint)
                    || FALLBACK_ROOT_EC_2025_SHA256.equals(fingerprint);
        }
        if (!trusted) {
            Log.w(TAG, "Untrusted attestation root, fingerprint="
                    + fingerprint + " liveAnchors=" + anchors.size());
            return "UNTRUSTED_ROOT";
        }
        return null;
    }

    /** Hex without leading zeros, the same hex in upper case, and the decimal form. */
    private static List<String> serialKeys(List<X509Certificate> certs) {
        LinkedHashSet<String> serials = new LinkedHashSet<>();
        for (X509Certificate cert : certs) {
            String hex = cert.getSerialNumber().toString(16);
            serials.add(hex.toLowerCase(Locale.US));
            serials.add(hex.toUpperCase(Locale.US));
            serials.add(cert.getSerialNumber().toString());
        }
        return new ArrayList<>(serials);
    }

    private static JSONObject fetchRevocationJson() {
        String body = fetchText(REVOCATION_URL);
        if (body == null) return null;
        try {
            return new JSONObject(body);
        } catch (Exception e) {
            Log.w(TAG, "Revocation list was not JSON", e);
            return null;
        }
    }

    private static Set<X509Certificate> getTrustAnchors(Context context) {
        SharedPreferences prefs = prefs(context);
        String cached = prefs.getString(KEY_ROOTS, null);
        long cachedAt = prefs.getLong(KEY_ROOTS_AT, 0L);
        long now = System.currentTimeMillis();
        boolean fresh = cached != null && !cached.isEmpty()
                && cachedAt > 0L && cachedAt <= now
                && now - cachedAt <= ROOTS_CACHE_TTL_MS;
        if (fresh) {
            Set<X509Certificate> parsed = parseRoots(cached);
            if (parsed != null) return parsed;
        }

        String live = fetchText(ROOTS_URL);
        if (live != null) {
            Set<X509Certificate> parsed = parseRoots(live);
            if (parsed != null) {
                prefs.edit().putString(KEY_ROOTS, live).putLong(KEY_ROOTS_AT, now).apply();
                return parsed;
            }
        }

        if (cached != null && !cached.isEmpty()) {
            Set<X509Certificate> parsed = parseRoots(cached);
            if (parsed != null) return parsed;
        }
        return Collections.emptySet();
    }

    private static Set<X509Certificate> parseRoots(String raw) {
        try {
            JSONArray array = new JSONArray(raw.trim());
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            Set<X509Certificate> certs = new LinkedHashSet<>();
            for (int i = 0; i < array.length(); i++) {
                try {
                    String pem = array.getString(i);
                    certs.add((X509Certificate) factory.generateCertificate(
                            new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8))));
                } catch (Exception ignored) {
                    // Skip one bad entry and keep the rest.
                }
            }
            return certs.isEmpty() ? null : certs;
        } catch (Exception e) {
            Log.w(TAG, "Attestation roots response was not usable", e);
            return null;
        }
    }

    private static void cacheRevokedSerial(Context context, String serial, String status) {
        prefs(context).edit()
                .putString(KEY_REVOKED_SERIAL, serial)
                .putString(KEY_REVOKED_STATUS, status)
                .putLong(KEY_REVOKED_AT, System.currentTimeMillis())
                .apply();
    }

    private static void clearCachedRevocation(Context context) {
        prefs(context).edit()
                .remove(KEY_REVOKED_SERIAL)
                .remove(KEY_REVOKED_STATUS)
                .remove(KEY_REVOKED_AT)
                .apply();
    }

    /**
     * Used when the live status list cannot be trusted. Reports a previously seen
     * revoked or suspended serial inside the trust window, and never reports valid.
     */
    private static Result checkCachedRevocation(Context context, List<String> serials,
            long leafExpiry, String sourceId) {
        SharedPreferences prefs = prefs(context);
        long cachedAt = prefs.getLong(KEY_REVOKED_AT, 0L);
        if (cachedAt == 0L || System.currentTimeMillis() - cachedAt > CACHE_TRUST_WINDOW_MS) {
            return null;
        }
        String cachedSerial = prefs.getString(KEY_REVOKED_SERIAL, "");
        if (cachedSerial == null || cachedSerial.isEmpty() || !serials.contains(cachedSerial)) {
            return null;
        }
        String state = prefs.getString(KEY_REVOKED_STATUS, "SUSPENDED");
        Status status = "REVOKED".equals(state) ? Status.REVOKED : Status.SUSPENDED;
        return new Result(status, "CACHED_OFFLINE", leafExpiry, sourceId);
    }

    private static String fetchText(String url) {
        java.net.HttpURLConnection connection = null;
        try {
            String busted = url + (url.indexOf('?') >= 0 ? "&" : "?") + "_=" + System.nanoTime();
            connection = (java.net.HttpURLConnection) new java.net.URL(busted).openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Cache-Control", "max-age=0");
            connection.setRequestProperty("Accept-Encoding", "identity");
            if (connection.getResponseCode() != java.net.HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Fetch failed for attestation endpoint, HTTP "
                        + connection.getResponseCode());
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line).append('\n');
                }
                return body.toString();
            }
        } catch (Exception e) {
            Log.w(TAG, "Attestation endpoint fetch failed", e);
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String sanitize(String xml) {
        return xml.replace("\uFEFF", "")
                .replace("\u200B", "")
                .replace("\u200C", "")
                .replace("\u200D", "");
    }

    private static String joinChains(List<List<X509Certificate>> chains) {
        StringBuilder builder = new StringBuilder();
        for (List<X509Certificate> chain : chains) {
            for (X509Certificate cert : chain) {
                try {
                    builder.append(sha256Hex(cert.getEncoded())).append('\n');
                } catch (Exception ignored) {
                    builder.append(cert.getSerialNumber().toString(16)).append('\n');
                }
            }
        }
        return builder.toString();
    }

    private static String sourceId(String material) {
        return sha256Hex(material.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(byte[] input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input);
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                builder.append(String.format(Locale.US, "%02X", value));
            }
            return builder.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
