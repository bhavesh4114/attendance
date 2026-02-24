package com.example.majuri_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores and validates registered users (mobile + password) using SharedPreferences.
 * Stored as a single string to avoid StringSet persistence issues on some devices.
 */
public class AuthHelper {

    private static final String PREF_NAME = "MajuriAppAuth";
    // Use a new key to avoid type conflicts with older StringSet-based storage
    private static final String KEY_USERS = "registered_users_v2";  // "mobile|||password|||name|||role"
    private static final String SEP = "|||";
    private static final char ENTRY_SEP = '\u0001';  // character unlikely in user input
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_USER = "user";

    private final SharedPreferences prefs;

    public AuthHelper(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Register a new user. Returns false if mobile already exists.
     * role = "admin" or "user"
     */
    public boolean registerUser(String mobile, String password, String fullName, String role) {
        String normalized = normalizeMobile(mobile);
        if (normalized.isEmpty()) return false;
        List<String> users = getUsersList();
        for (String entry : users) {
            if (entry.startsWith(normalized + SEP)) return false; // mobile already registered
        }
        String r = (ROLE_ADMIN.equals(role) || ROLE_USER.equals(role)) ? role : ROLE_USER;
        users.add(normalized + SEP + password + SEP + (fullName != null ? fullName : "") + SEP + r);
        String saved = TextUtils.join(String.valueOf(ENTRY_SEP), users);
        prefs.edit().putString(KEY_USERS, saved).commit();
        return true;
    }

    /**
     * Validate credentials. Returns true if mobile and password match a registered user.
     */
    public boolean validateUser(String mobile, String password) {
        if (password == null || password.isEmpty()) return false;
        String normalized = normalizeMobile(mobile);
        List<String> users = getUsersList();
        for (String entry : users) {
            if (entry.startsWith(normalized + SEP)) {
                String[] parts = entry.split(SEP, 4);
                if (parts.length >= 2 && parts[1].equals(password)) return true;
                return false;
            }
        }
        return false;
    }

    /**
     * Get role for a mobile: "admin" or "user". Default "user" if not stored.
     */
    public String getRole(String mobile) {
        String normalized = normalizeMobile(mobile);
        List<String> users = getUsersList();
        for (String entry : users) {
            if (entry.startsWith(normalized + SEP)) {
                String[] parts = entry.split(SEP, 4);
                return parts.length >= 4 && ROLE_ADMIN.equals(parts[3]) ? ROLE_ADMIN : ROLE_USER;
            }
        }
        return ROLE_USER;
    }

    /**
     * Get stored full name for a mobile (for session).
     */
    public String getUserName(String mobile) {
        String normalized = normalizeMobile(mobile);
        List<String> users = getUsersList();
        for (String entry : users) {
            if (entry.startsWith(normalized + SEP)) {
                String[] parts = entry.split(SEP, 4);
                return parts.length >= 3 ? parts[2] : "";
            }
        }
        return "";
    }

    public boolean isUserRegistered(String mobile) {
        String normalized = normalizeMobile(mobile);
        List<String> users = getUsersList();
        for (String entry : users) {
            if (entry.startsWith(normalized + SEP)) return true;
        }
        return false;
    }

    private List<String> getUsersList() {
        String raw = prefs.getString(KEY_USERS, "");
        if (raw == null || raw.isEmpty()) return new ArrayList<>();
        List<String> list = new ArrayList<>();
        for (String entry : raw.split(String.valueOf(ENTRY_SEP))) {
            if (entry != null && !entry.trim().isEmpty()) list.add(entry);
        }
        return list;
    }

    /**
     * Normalize to 10-digit Indian mobile (strip +91 / 91 prefix if present).
     */
    private static String normalizeMobile(String mobile) {
        if (mobile == null) return "";
        String digits = mobile.trim().replaceAll("\\s+", "").replaceAll("[^0-9]", "");
        if (digits.length() == 11 && digits.startsWith("91")) return digits.substring(2);
        if (digits.length() == 10) return digits;
        return digits;
    }
}
