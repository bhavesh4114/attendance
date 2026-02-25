package com.example.majuri_app;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

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
}
