package com.example.majuri_app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite helper for saving and loading workers.
 */
public class WorkerDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "majuri_workers.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE_WORKERS = "workers";

    public static final String COL_ID = "id";
    public static final String COL_NAME = "name";
    public static final String COL_MOBILE = "mobile";
    public static final String COL_DAILY_WAGE = "daily_wage";
    public static final String COL_SKILL = "skill";

    public WorkerDbHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_WORKERS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_NAME + " TEXT NOT NULL, "
                + COL_MOBILE + " TEXT NOT NULL, "
                + COL_DAILY_WAGE + " TEXT NOT NULL, "
                + COL_SKILL + " TEXT NOT NULL)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WORKERS);
        onCreate(db);
    }

    /**
     * Insert a new worker. Returns the row id, or -1 on error.
     */
    public long insertWorker(String name, String mobile, String dailyWage, String skill) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_NAME, name != null ? name : "");
        cv.put(COL_MOBILE, mobile != null ? mobile : "");
        cv.put(COL_DAILY_WAGE, dailyWage != null ? dailyWage : "");
        cv.put(COL_SKILL, skill != null ? skill : "");
        long id = db.insert(TABLE_WORKERS, null, cv);
        db.close();
        return id;
    }

    /**
     * Returns total number of workers in the database (from backend).
     */
    public int getWorkersCount() {
        SQLiteDatabase db = getReadableDatabase();
        android.database.Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_WORKERS, null);
        int count = 0;
        if (c != null && c.moveToFirst()) {
            count = c.getInt(0);
            c.close();
        }
        db.close();
        return count;
    }

    /**
     * Returns all workers from the database (backend) for the Workers list screen.
     */
    public List<WorkerListItem> getAllWorkers() {
        List<WorkerListItem> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_WORKERS, null, null, null, null, null, COL_ID + " DESC");
        if (c != null) {
            int iId = c.getColumnIndex(COL_ID);
            int iName = c.getColumnIndex(COL_NAME);
            int iMobile = c.getColumnIndex(COL_MOBILE);
            int iSkill = c.getColumnIndex(COL_SKILL);
            while (c.moveToNext()) {
                long id = iId >= 0 ? c.getLong(iId) : -1L;
                String name = iName >= 0 ? c.getString(iName) : "";
                String mobile = iMobile >= 0 ? c.getString(iMobile) : "";
                String skill = iSkill >= 0 ? c.getString(iSkill) : "";
                list.add(new WorkerListItem(id, name, skill, mobile, true));
            }
            c.close();
        }
        db.close();
        return list;
    }

    /**
     * Returns a single worker by primary key id, or null if not found.
     */
    public WorkerProfile getWorkerProfileById(long workerId) {
        SQLiteDatabase db = getReadableDatabase();
        String[] args = { String.valueOf(workerId) };
        Cursor c = db.query(TABLE_WORKERS, null, COL_ID + "=?", args, null, null, null, "1");

        WorkerProfile profile = null;
        if (c != null) {
            int iId = c.getColumnIndex(COL_ID);
            int iName = c.getColumnIndex(COL_NAME);
            int iMobile = c.getColumnIndex(COL_MOBILE);
            int iSkill = c.getColumnIndex(COL_SKILL);
            if (c.moveToFirst()) {
                long id = iId >= 0 ? c.getLong(iId) : workerId;
                String name = iName >= 0 ? c.getString(iName) : "";
                String mobile = iMobile >= 0 ? c.getString(iMobile) : "";
                String skill = iSkill >= 0 ? c.getString(iSkill) : "";
                profile = new WorkerProfile(
                        id,
                        name,
                        skill,
                        mobile,
                        "",
                        true,
                        "",
                        "",
                        "",
                        ""
                );
            }
            c.close();
        }
        db.close();
        return profile;
    }
}
