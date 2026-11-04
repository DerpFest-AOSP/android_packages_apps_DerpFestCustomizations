/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package org.derpfest.customizations.keybox;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;

/**
 * Posts one notification when the active keybox fails validation, and removes it
 * once a later check says the keybox is valid again. The same failure is not
 * posted twice. A check that could not reach Google leaves any existing
 * notification alone.
 */
public final class KeyboxStatusNotifier {

    private static final String TAG = "KeyboxStatusNotifier";
    private static final String PREFS = "keybox_status";
    private static final String KEY_NOTIFIED = "notified_token";
    private static final String CHANNEL_ID = "keybox_status";
    private static final int NOTIFICATION_ID = 0x4B42;

    private KeyboxStatusNotifier() {}

    public static void apply(Context context, KeyboxChecker.Result raw) {
        if (raw == null || raw.status == KeyboxChecker.Status.CHECKING
                || raw.status == KeyboxChecker.Status.UNKNOWN) {
            return;
        }
        Context app = context.getApplicationContext();
        KeyboxChecker.Result result = raw.at(System.currentTimeMillis());
        NotificationManager manager = app.getSystemService(NotificationManager.class);
        if (manager == null) return;

        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!result.notifies()) {
            manager.cancel(NOTIFICATION_ID);
            prefs.edit().remove(KEY_NOTIFIED).apply();
            return;
        }

        String token = result.status.name() + "|" + result.detail + "|" + result.sourceId;
        if (token.equals(prefs.getString(KEY_NOTIFIED, null))) return;

        try {
            ensureChannel(app, manager);
            Notification.Builder builder = new Notification.Builder(app, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_keybox_notification)
                    .setContentTitle(app.getString(R.string.keybox_notification_title))
                    .setContentText(message(app, result))
                    .setStyle(new Notification.BigTextStyle().bigText(message(app, result)))
                    .setCategory(Notification.CATEGORY_ERROR)
                    .setAutoCancel(true)
                    .setContentIntent(contentIntent(app));
            manager.notify(NOTIFICATION_ID, builder.build());
            prefs.edit().putString(KEY_NOTIFIED, token).apply();
        } catch (Exception e) {
            Log.w(TAG, "Failed to post keybox notification", e);
        }
    }

    private static void ensureChannel(Context context, NotificationManager manager) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.keybox_notification_channel),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.keybox_notification_channel_description));
        manager.createNotificationChannel(channel);
    }

    private static PendingIntent contentIntent(Context context) {
        android.content.Intent intent = new SubSettingLauncher(context)
                .setDestination("org.derpfest.customizations.fragment.Spoofing")
                .setTitleRes(R.string.spoofing_title)
                .setSourceMetricsCategory(MetricsProto.MetricsEvent.DERPFEST)
                .toIntent();
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, NOTIFICATION_ID, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static String message(Context context, KeyboxChecker.Result result) {
        switch (result.status) {
            case REVOKED:
                return context.getString(R.string.keybox_notification_revoked);
            case SUSPENDED:
                return context.getString(R.string.keybox_notification_suspended);
            case EXPIRED:
                return context.getString(R.string.keybox_notification_expired);
            case UNTRUSTED_ROOT:
                return context.getString(R.string.keybox_notification_untrusted);
            case CHAIN_INVALID:
            default:
                return context.getString(R.string.keybox_notification_invalid);
        }
    }
}
