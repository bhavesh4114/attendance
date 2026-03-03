package com.example.majuri_app;

import android.content.Context;
import android.content.SharedPreferences;

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
        List<AppNotification> list = getNotifications(context);
        List<AppNotification> updated = new ArrayList<>();

        updated.add(new AppNotification(
                System.currentTimeMillis(),
                safe(title, "Notification"),
                safe(message, ""),
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
}
