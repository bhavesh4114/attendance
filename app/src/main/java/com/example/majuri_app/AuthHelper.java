package com.example.majuri_app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Stores and validates registered users (mobile + password) using SharedPreferences.
 * Used for login and sign up until you switch to a real backend or database.
 */
public class AuthHelper {

    private static final String PREF_NAME = "MajuriAppAuth";
    private static final String KEY_USERS = "registered_users";  // "mobile:password:name"
    private static final String SEP = "|||";

    private final SharedPreferences prefs;

    public AuthHelper(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Register a new user. Returns false if mobile already exists.
     */
    public boolean registerUser(String mobile, String password, String fullName) {
        String normalized = normalizeMobile(mobile);
        if (normalized.isEmpty()) return false;
        Set<String> users = getUsersSet();
        for (String entry : users) {
            if (entry.startsWith(normalized + SEP)) return false; // mobile already registered
        }
        users.add(normalized + SEP + password + SEP + (fullName != null ? fullName : ""));
        prefs.edit().putStringSet(KEY_USERS, users).apply();
        return true;
    }

    /**
     * Validate credentials. Returns true if mobile and password match a registered user.
     */
    public boolean validateUser(String mobile, String password) {
        if (password == null || password.isEmpty()) return false;
        String normalized = normalizeMobile(mobile);
        Set<String> users = getUsersSet();
        String expected = normalized + SEP + password;
        for (String entry : users) {
            if (entry.startsWith(normalized + SEP)) {
                String[] parts = entry.split(SEP, 3);
                if (parts.length >= 2 && parts[1].equals(password)) return true;
                return false;
            }
        }
        return false;
    }

    /**
     * Get stored full name for a mobile (for session).
     */
    public String getUserName(String mobile) {
        String normalized = normalizeMobile(mobile);
        Set<String> users = getUsersSet();
        for (String entry : users) {
            if (entry.startsWith(normalized + SEP)) {
                String[] parts = entry.split(SEP, 3);
                return parts.length >= 3 ? parts[2] : "";
            }
        }
        return "";
    }

    public boolean isUserRegistered(String mobile) {
        String normalized = normalizeMobile(mobile);
        Set<String> users = getUsersSet();
        for (String entry : users) {
            if (entry.startsWith(normalized + SEP)) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Set<String> getUsersSet() {
        Set<String> set = prefs.getStringSet(KEY_USERS, null);
        return set != null ? new HashSet<>(set) : new HashSet<>();
    }

    private static String normalizeMobile(String mobile) {
        if (mobile == null) return "";
        return mobile.trim().replaceAll("\\s+", "").replaceAll("[^0-9]", "");
    }
}
