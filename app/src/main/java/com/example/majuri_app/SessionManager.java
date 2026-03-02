package com.example.majuri_app;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages login session using SharedPreferences.
 * Use to check if user is logged in and to logout.
 */
public class SessionManager {

    private static final String PREF_NAME = "MajuriAppSession";
    private static final String IS_LOGIN = "IS_LOGIN";
    private static final String USER_MOBILE = "USER_MOBILE";
    private static final String KEY_USER_NAME = "user_name";
    private static final String IS_ADMIN = "IS_ADMIN";
    private static final String USER_ROLE = "USER_ROLE";

    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_USER = "user";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    /**
     * Creates login session. Used on successful login.
     * isAdmin = true for admin dashboard, false for user dashboard.
     */
    public void createLoginSession(String mobile, boolean isAdmin) {
        saveSession(mobile, "", isAdmin ? ROLE_ADMIN : ROLE_USER);
    }

    public void saveSession(String mobile, String name, boolean isAdmin) {
        saveSession(mobile, name, isAdmin ? ROLE_ADMIN : ROLE_USER);
    }

    public void saveSession(String mobile, String name, String role) {
        String normalizedRole = ROLE_ADMIN.equalsIgnoreCase(role) ? ROLE_ADMIN : ROLE_USER;
        editor.putBoolean(IS_LOGIN, true);
        editor.putString(USER_MOBILE, mobile != null ? mobile : "");
        editor.putString(KEY_USER_NAME, name != null ? name : "");
        editor.putBoolean(IS_ADMIN, ROLE_ADMIN.equals(normalizedRole));
        editor.putString(USER_ROLE, normalizedRole);
        editor.apply();
    }

    public boolean isAdmin() {
        return prefs.getBoolean(IS_ADMIN, false);
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(IS_LOGIN, false);
    }

    public String getLoggedInMobile() {
        return prefs.getString(USER_MOBILE, "");
    }

    public String getLoggedInUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }

    public String getUserRole() {
        String role = prefs.getString(USER_ROLE, null);
        if (role == null || role.trim().isEmpty()) {
            return isAdmin() ? ROLE_ADMIN : ROLE_USER;
        }
        return ROLE_ADMIN.equalsIgnoreCase(role) ? ROLE_ADMIN : ROLE_USER;
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}
