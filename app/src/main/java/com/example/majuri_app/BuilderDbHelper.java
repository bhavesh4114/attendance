package com.example.majuri_app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite helper for saving builder/admin registration data.
 */
public class BuilderDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "majuri_builders.db";
    private static final int DB_VERSION = 4;
    private static final String TABLE_BUILDERS = "builders";

    private static final String COL_ID = "id";
    private static final String COL_FULL_NAME = "full_name";
    private static final String COL_MOBILE = "mobile";
    private static final String COL_BUSINESS_NAME = "business_name";
    private static final String COL_EMAIL = "email";
    private static final String COL_PASSWORD = "password";

    public BuilderDbHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_BUILDERS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_FULL_NAME + " TEXT NOT NULL, "
                + COL_MOBILE + " TEXT NOT NULL, "
                + COL_BUSINESS_NAME + " TEXT, "
                + COL_EMAIL + " TEXT, "
                + COL_PASSWORD + " TEXT NOT NULL)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_BUILDERS + " ADD COLUMN " + COL_EMAIL + " TEXT");
            } catch (Exception ignored) {
                // Column may already exist on some installs.
            }
        }
    }

    /**
     * Insert builder registration. Returns row id or -1 on error.
     */
    public long insertBuilder(String fullName, String mobile, String businessName, String email, String password) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_FULL_NAME, fullName != null ? fullName : "");
        cv.put(COL_MOBILE, mobile != null ? mobile : "");
        cv.put(COL_BUSINESS_NAME, businessName != null ? businessName : "");
        cv.put(COL_EMAIL, email != null ? email.trim().toLowerCase() : "");
        cv.put(COL_PASSWORD, password != null ? password : "");
        long id = db.insert(TABLE_BUILDERS, null, cv);
        db.close();
        return id;
    }

    /**
     * Check if a builder already exists for given mobile.
     */
    public boolean isBuilderExists(String mobile) {
        SQLiteDatabase db = getReadableDatabase();
        String[] columns = { COL_ID };
        String[] args = { mobile };
        android.database.Cursor cursor = db.query(TABLE_BUILDERS, columns,
                COL_MOBILE + "=?", args, null, null, null);
        boolean exists = cursor != null && cursor.moveToFirst();
        if (cursor != null) cursor.close();
        db.close();
        return exists;
    }

    /**
     * Validate login and return full name if mobile+password are correct, otherwise empty string.
     */
    public String getBuilderNameIfValid(String mobile, String password) {
        SQLiteDatabase db = getReadableDatabase();
        String[] columns = { COL_FULL_NAME };
        String[] args = { mobile, password };
        android.database.Cursor cursor = db.query(TABLE_BUILDERS, columns,
                COL_MOBILE + "=? AND " + COL_PASSWORD + "=?", args,
                null, null, null);
        String name = "";
        if (cursor != null && cursor.moveToFirst()) {
            int idx = cursor.getColumnIndex(COL_FULL_NAME);
            if (idx >= 0) {
                name = cursor.getString(idx);
            }
        }
        if (cursor != null) cursor.close();
        db.close();
        return name != null ? name : "";
    }

    /**
     * Validate login and return full name if email+password are correct, otherwise empty string.
     */
    public String getBuilderNameIfValidByEmail(String email, String password) {
        SQLiteDatabase db = getReadableDatabase();
        String[] columns = { COL_FULL_NAME };
        String[] args = { email != null ? email.trim().toLowerCase() : "", password };
        android.database.Cursor cursor = db.query(
                TABLE_BUILDERS,
                columns,
                "LOWER(" + COL_EMAIL + ")=? AND " + COL_PASSWORD + "=?",
                args,
                null,
                null,
                null
        );
        String name = "";
        if (cursor != null && cursor.moveToFirst()) {
            int idx = cursor.getColumnIndex(COL_FULL_NAME);
            if (idx >= 0) {
                name = cursor.getString(idx);
            }
        }
        if (cursor != null) cursor.close();
        db.close();
        return name != null ? name : "";
    }

    /**
     * Returns distinct business/site names for dropdown use.
     */
    public List<String> getAllBusinessNames() {
        List<String> sites = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                true,
                TABLE_BUILDERS,
                new String[]{COL_BUSINESS_NAME},
                COL_BUSINESS_NAME + " IS NOT NULL AND TRIM(" + COL_BUSINESS_NAME + ") != ''",
                null,
                null,
                null,
                COL_BUSINESS_NAME + " COLLATE NOCASE ASC",
                null
        );
        if (cursor != null) {
            int index = cursor.getColumnIndex(COL_BUSINESS_NAME);
            while (cursor.moveToNext()) {
                String site = index >= 0 ? cursor.getString(index) : "";
                if (site != null && !site.trim().isEmpty()) {
                    sites.add(site.trim());
                }
            }
            cursor.close();
        }
        db.close();
        return sites;
    }

    /**
     * Returns preferred display name (business name first, then full name) for the login id.
     * loginId can be mobile number or email.
     */
    public String getBuilderDisplayName(String loginId) {
        if (loginId == null || loginId.trim().isEmpty()) {
            return "";
        }

        String trimmed = loginId.trim();
        String normalizedMobile = normalizeMobile(trimmed);
        String normalizedEmail = trimmed.toLowerCase();

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            if (!normalizedMobile.isEmpty()) {
                cursor = db.query(
                        TABLE_BUILDERS,
                        new String[]{COL_BUSINESS_NAME, COL_FULL_NAME},
                        COL_MOBILE + "=?",
                        new String[]{normalizedMobile},
                        null,
                        null,
                        COL_ID + " DESC",
                        "1"
                );
            }

            if ((cursor == null || !cursor.moveToFirst()) && normalizedEmail.contains("@")) {
                if (cursor != null) {
                    cursor.close();
                }
                cursor = db.query(
                        TABLE_BUILDERS,
                        new String[]{COL_BUSINESS_NAME, COL_FULL_NAME},
                        "LOWER(" + COL_EMAIL + ")=?",
                        new String[]{normalizedEmail},
                        null,
                        null,
                        COL_ID + " DESC",
                        "1"
                );
            }

            if (cursor != null && cursor.moveToFirst()) {
                String business = safeGet(cursor, COL_BUSINESS_NAME);
                String fullName = safeGet(cursor, COL_FULL_NAME);
                if (!business.isEmpty()) return business;
                return fullName;
            }
            return "";
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }

    private String safeGet(Cursor cursor, String column) {
        if (cursor == null) return "";
        int index = cursor.getColumnIndex(column);
        if (index < 0) return "";
        String value = cursor.getString(index);
        return value != null ? value.trim() : "";
    }

    private String normalizeMobile(String value) {
        if (value == null) return "";
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() < 10) return "";
        return digits.substring(digits.length() - 10);
    }

    /**
     * Verifies builder password for current logged-in login id (mobile or email).
     */
    public boolean isBuilderPasswordValid(String loginId, String password) {
        if (TextUtils.isEmpty(loginId) || TextUtils.isEmpty(password)) return false;

        String trimmed = loginId.trim();
        String normalizedMobile = normalizeMobile(trimmed);
        String normalizedEmail = trimmed.toLowerCase();

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            if (!normalizedMobile.isEmpty()) {
                cursor = db.query(
                        TABLE_BUILDERS,
                        new String[]{COL_ID},
                        COL_MOBILE + "=? AND " + COL_PASSWORD + "=?",
                        new String[]{normalizedMobile, password},
                        null,
                        null,
                        null,
                        "1"
                );
                if (cursor != null && cursor.moveToFirst()) {
                    return true;
                }
            }

            if (cursor != null) {
                cursor.close();
            }
            cursor = db.query(
                    TABLE_BUILDERS,
                    new String[]{COL_ID},
                    "LOWER(" + COL_EMAIL + ")=? AND " + COL_PASSWORD + "=?",
                    new String[]{normalizedEmail, password},
                    null,
                    null,
                    null,
                    "1"
            );
            return cursor != null && cursor.moveToFirst();
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }

    /**
     * Updates builder password for current logged-in login id (mobile or email).
     */
    public boolean updateBuilderPassword(String loginId, String newPassword) {
        if (TextUtils.isEmpty(loginId) || TextUtils.isEmpty(newPassword)) return false;

        String trimmed = loginId.trim();
        String normalizedMobile = normalizeMobile(trimmed);
        String normalizedEmail = trimmed.toLowerCase();

        SQLiteDatabase db = getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_PASSWORD, newPassword);

            int updated = 0;
            if (!normalizedMobile.isEmpty()) {
                updated = db.update(TABLE_BUILDERS, cv, COL_MOBILE + "=?", new String[]{normalizedMobile});
            }
            if (updated <= 0) {
                updated = db.update(
                        TABLE_BUILDERS,
                        cv,
                        "LOWER(" + COL_EMAIL + ")=?",
                        new String[]{normalizedEmail}
                );
            }
            return updated > 0;
        } finally {
            db.close();
        }
    }
}
