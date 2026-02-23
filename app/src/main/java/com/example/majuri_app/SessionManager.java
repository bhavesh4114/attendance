package com.example.majuri_app;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages login session using SharedPreferences.
 * Use to check if user is logged in and to logout.
 */
public class SessionManager {

    private static final String PREF_NAME = "MajuriAppSession";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_MOBILE = "user_mobile";
    private static final String KEY_USER_NAME = "user_name";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveSession(String mobile, String name) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_MOBILE, mobile);
        editor.putString(KEY_USER_NAME, name != null ? name : "");
        editor.apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getLoggedInMobile() {
        return prefs.getString(KEY_USER_MOBILE, "");
    }

    public String getLoggedInUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}
