package com.example.majuri_app;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class NotificationStore {

    private static final String PREF_NAME = "MajuriAppNotifications";
    private static final String KEY_ITEMS = "items";
    private static final int MAX_ITEMS = 30;
    private static final String CHANNEL_ID_GENERAL = "majuri_general_alerts";
    private static final String CHANNEL_NAME_GENERAL = "General Alerts";

    private NotificationStore() {
    }

    public static void seedIfEmpty(Context context) {
        List<AppNotification> existing = getNotifications(context);
        if (!existing.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        List<AppNotification> seed = new ArrayList<>();
        seed.add(new AppNotification(
                now,
                "Attendance Reminder",
                "Today attendance summary is ready for review.",
                "Today",
                true
        ));
        seed.add(new AppNotification(
                now - 1,
                "Payment Alert",
                "Pending payments list has been updated.",
                "Today",
                true
        ));
        seed.add(new AppNotification(
                now - 2,
                "Monthly Report",
                "Your worker report PDF can be downloaded now.",
                "Yesterday",
                true
        ));
        saveNotifications(context, seed);
    }

    public static List<AppNotification> getNotifications(Context context) {
        JSONArray array = readArray(context);
        List<AppNotification> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.optJSONObject(i);
            if (object == null) {
                continue;
            }

            long id = object.optLong("id", System.currentTimeMillis() + i);
            String title = object.optString("title", "Notification");
            String message = object.optString("message", "");
            String timeLabel = object.optString("timeLabel", "");
            boolean unread = object.optBoolean("unread", false);
            list.add(new AppNotification(id, title, message, timeLabel, unread));
        }
        return list;
    }

    public static boolean hasUnread(Context context) {
        List<AppNotification> list = getNotifications(context);
        for (AppNotification item : list) {
            if (item.isUnread()) {
                return true;
            }
        }
        return false;
    }

    public static void markAllRead(Context context) {
        List<AppNotification> list = getNotifications(context);
        List<AppNotification> updated = new ArrayList<>(list.size());
        for (AppNotification item : list) {
            updated.add(new AppNotification(
                    item.getId(),
                    item.getTitle(),
                    item.getMessage(),
                    item.getTimeLabel(),
                    false
            ));
        }
        saveNotifications(context, updated);
    }

    public static void pushNotification(Context context, String title, String message) {
        String safeTitle = safe(title, "Notification");
        String safeMessage = safe(message, "");
        List<AppNotification> list = getNotifications(context);
        List<AppNotification> updated = new ArrayList<>();

        updated.add(new AppNotification(
                System.currentTimeMillis(),
                safeTitle,
                safeMessage,
                new SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(new Date()),
                true
        ));

        for (AppNotification item : list) {
            if (updated.size() >= MAX_ITEMS) {
                break;
            }
            updated.add(item);
        }

        saveNotifications(context, updated);
        showSystemNotification(context, safeTitle, safeMessage);
    }

    private static String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private static JSONArray readArray(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_ITEMS, "[]");
        try {
            return new JSONArray(json);
        } catch (JSONException ignored) {
            return new JSONArray();
        }
    }

    private static void saveNotifications(Context context, List<AppNotification> list) {
        JSONArray array = new JSONArray();
        for (int i = 0; i < list.size() && i < MAX_ITEMS; i++) {
            AppNotification item = list.get(i);
            JSONObject object = new JSONObject();
            try {
                object.put("id", item.getId());
                object.put("title", item.getTitle());
                object.put("message", item.getMessage());
                object.put("timeLabel", item.getTimeLabel());
                object.put("unread", item.isUnread());
                array.put(object);
            } catch (JSONException ignored) {
                // Skip malformed notification payload.
            }
        }

        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ITEMS, array.toString()).apply();
    }

    private static void showSystemNotification(Context context, String title, String message) {
        Context appContext = context.getApplicationContext();
        if (!hasNotificationPermission(appContext)) {
            return;
        }

        NotificationManager notificationManager =
                (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        ensureNotificationChannel(notificationManager);

        Intent openIntent = new Intent(appContext, NotificationActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(
                appContext,
                0,
                openIntent,
                pendingIntentFlags
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID_GENERAL)
                .setSmallIcon(R.drawable.ic_notifications_bell)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        int notificationId = (int) (System.currentTimeMillis() & 0x7FFFFFFF);
        NotificationManagerCompat.from(appContext).notify(notificationId, builder.build());
    }

    private static void ensureNotificationChannel(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel existing = notificationManager.getNotificationChannel(CHANNEL_ID_GENERAL);
        if (existing != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID_GENERAL,
                CHANNEL_NAME_GENERAL,
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Attendance, payments and report alerts.");
        channel.enableVibration(true);
        notificationManager.createNotificationChannel(channel);
    }

    private static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED;
    }
}
