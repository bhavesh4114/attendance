package com.example.majuri_app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SQLite helper for saving workers and payroll-related data.
 */
public class WorkerDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "majuri_workers.db";
    private static final int DB_VERSION = 2;

    private static final String TABLE_WORKERS = "workers";
    private static final String TABLE_ATTENDANCE = "attendance_records";
    private static final String TABLE_ADVANCES = "payment_advances";

    public static final String COL_ID = "id";
    public static final String COL_NAME = "name";
    public static final String COL_MOBILE = "mobile";
    public static final String COL_DAILY_WAGE = "daily_wage";
    public static final String COL_SKILL = "skill";

    private static final String COL_ATTENDANCE_WORKER_ID = "worker_id";
    private static final String COL_ATTENDANCE_DATE = "attendance_date";
    private static final String COL_ATTENDANCE_STATUS = "status";
    private static final String COL_ATTENDANCE_LOCKED = "locked";

    private static final String COL_ADVANCE_WORKER_ID = "worker_id";
    private static final String COL_ADVANCE_AMOUNT = "amount";
    private static final String COL_ADVANCE_DATE = "payment_date";
    private static final String COL_ADVANCE_NOTE = "note";

    public WorkerDbHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createWorkersTable(db);
        createAttendanceTable(db);
        createAdvancesTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createAttendanceTable(db);
            createAdvancesTable(db);
        }
    }

    private void createWorkersTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_WORKERS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_NAME + " TEXT NOT NULL, "
                + COL_MOBILE + " TEXT NOT NULL, "
                + COL_DAILY_WAGE + " TEXT NOT NULL, "
                + COL_SKILL + " TEXT NOT NULL)";
        db.execSQL(sql);
    }

    private void createAttendanceTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_ATTENDANCE + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_ATTENDANCE_WORKER_ID + " INTEGER NOT NULL, "
                + COL_ATTENDANCE_DATE + " TEXT NOT NULL, "
                + COL_ATTENDANCE_STATUS + " INTEGER NOT NULL, "
                + COL_ATTENDANCE_LOCKED + " INTEGER NOT NULL DEFAULT 1, "
                + "UNIQUE(" + COL_ATTENDANCE_WORKER_ID + ", " + COL_ATTENDANCE_DATE + "))";
        db.execSQL(sql);
    }

    private void createAdvancesTable(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_ADVANCES + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_ADVANCE_WORKER_ID + " INTEGER NOT NULL, "
                + COL_ADVANCE_AMOUNT + " REAL NOT NULL, "
                + COL_ADVANCE_DATE + " TEXT NOT NULL, "
                + COL_ADVANCE_NOTE + " TEXT)";
        db.execSQL(sql);
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
     * Returns total number of workers in the database.
     */
    public int getWorkersCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_WORKERS, null);
        int count = 0;
        if (c != null && c.moveToFirst()) {
            count = c.getInt(0);
            c.close();
        }
        db.close();
        return count;
    }

    /**
     * Returns all workers from the database for the Workers list screen.
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
     * Returns only worker names for lightweight dropdown use-cases.
     */
    public List<String> getAllWorkerNames() {
        List<String> names = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(
                TABLE_WORKERS,
                new String[]{COL_NAME},
                null,
                null,
                null,
                null,
                COL_NAME + " COLLATE NOCASE ASC"
        );
        if (c != null) {
            int iName = c.getColumnIndex(COL_NAME);
            while (c.moveToNext()) {
                String name = iName >= 0 ? c.getString(iName) : "";
                if (name != null && !name.trim().isEmpty()) {
                    names.add(name.trim());
                }
            }
            c.close();
        }
        db.close();
        return names;
    }

    /**
     * Returns distinct worker skill types for dropdowns.
     */
    public List<String> getAllSkillTypes() {
        List<String> skills = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(
                true,
                TABLE_WORKERS,
                new String[]{COL_SKILL},
                COL_SKILL + " IS NOT NULL AND TRIM(" + COL_SKILL + ") != ''",
                null,
                null,
                null,
                COL_SKILL + " COLLATE NOCASE ASC",
                null
        );
        if (c != null) {
            int iSkill = c.getColumnIndex(COL_SKILL);
            while (c.moveToNext()) {
                String skill = iSkill >= 0 ? c.getString(iSkill) : "";
                if (skill != null && !skill.trim().isEmpty()) {
                    skills.add(skill.trim());
                }
            }
            c.close();
        }
        db.close();
        return skills;
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

    /**
     * Returns workerId -> attendance status map for a specific date (yyyy-MM-dd).
     */
    public Map<Long, Integer> getAttendanceStatusByDate(String attendanceDate) {
        Map<Long, Integer> result = new HashMap<>();
        if (attendanceDate == null || attendanceDate.trim().isEmpty()) return result;

        SQLiteDatabase db = getReadableDatabase();
        String[] columns = { COL_ATTENDANCE_WORKER_ID, COL_ATTENDANCE_STATUS };
        String[] args = { attendanceDate.trim() };
        Cursor c = db.query(
                TABLE_ATTENDANCE,
                columns,
                COL_ATTENDANCE_DATE + "=?",
                args,
                null,
                null,
                null
        );

        if (c != null) {
            int iWorkerId = c.getColumnIndex(COL_ATTENDANCE_WORKER_ID);
            int iStatus = c.getColumnIndex(COL_ATTENDANCE_STATUS);
            while (c.moveToNext()) {
                long workerId = iWorkerId >= 0 ? c.getLong(iWorkerId) : -1L;
                int status = iStatus >= 0 ? c.getInt(iStatus) : AttendanceStaffItem.STATUS_PRESENT;
                if (workerId > 0L) {
                    result.put(workerId, status);
                }
            }
            c.close();
        }
        db.close();
        return result;
    }

    /**
     * True if attendance for a date is already locked (saved once).
     */
    public boolean isAttendanceLockedForDate(String attendanceDate) {
        if (attendanceDate == null || attendanceDate.trim().isEmpty()) return false;
        SQLiteDatabase db = getReadableDatabase();
        String[] args = { attendanceDate.trim() };
        Cursor c = db.rawQuery(
                "SELECT COUNT(1) FROM " + TABLE_ATTENDANCE
                        + " WHERE " + COL_ATTENDANCE_DATE + "=? AND " + COL_ATTENDANCE_LOCKED + "=1",
                args
        );
        boolean locked = false;
        if (c != null && c.moveToFirst()) {
            locked = c.getInt(0) > 0;
            c.close();
        }
        db.close();
        return locked;
    }

    /**
     * Save attendance rows for a date and lock them. Returns true on success.
     */
    public boolean saveAttendanceForDate(String attendanceDate, List<AttendanceStaffItem> items) {
        if (attendanceDate == null || attendanceDate.trim().isEmpty() || items == null || items.isEmpty()) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (AttendanceStaffItem item : items) {
                if (item == null || item.getWorkerDbId() <= 0L) continue;

                ContentValues cv = new ContentValues();
                cv.put(COL_ATTENDANCE_WORKER_ID, item.getWorkerDbId());
                cv.put(COL_ATTENDANCE_DATE, attendanceDate.trim());
                cv.put(COL_ATTENDANCE_STATUS, item.getStatus());
                cv.put(COL_ATTENDANCE_LOCKED, 1);

                long rowId = db.insertWithOnConflict(
                        TABLE_ATTENDANCE,
                        null,
                        cv,
                        SQLiteDatabase.CONFLICT_REPLACE
                );
                if (rowId == -1L) {
                    throw new IllegalStateException("Failed to save attendance row");
                }
            }
            db.setTransactionSuccessful();
            return true;
        } catch (Exception ignored) {
            return false;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /**
     * Record a payment/advance deduction entry for a worker.
     */
    public long recordAdvancePayment(long workerId, double amount, String paymentDate, String note) {
        if (workerId <= 0L || amount <= 0d || paymentDate == null || paymentDate.trim().isEmpty()) {
            return -1L;
        }

        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ADVANCE_WORKER_ID, workerId);
        cv.put(COL_ADVANCE_AMOUNT, amount);
        cv.put(COL_ADVANCE_DATE, paymentDate.trim());
        cv.put(COL_ADVANCE_NOTE, note != null ? note : "");
        long id = db.insert(TABLE_ADVANCES, null, cv);
        db.close();
        return id;
    }

    /**
     * Returns per-worker payment summary for the given month.
     * monthZeroBased follows Calendar.MONTH (0..11).
     */
    public List<WorkerPaymentSummary> getWorkerPaymentSummariesForMonth(int year, int monthZeroBased) {
        List<WorkerPaymentSummary> summaries = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        String monthPrefix = String.format(Locale.US, "%04d-%02d-%%", year, monthZeroBased + 1);
        Map<Long, Double> workedDaysMap = readWorkedDaysMap(db, monthPrefix);
        Map<Long, Double> paidMap = readPaidAmountMap(db, monthPrefix);

        Cursor c = db.query(TABLE_WORKERS, null, null, null, null, null, COL_ID + " DESC");
        if (c != null) {
            int iId = c.getColumnIndex(COL_ID);
            int iName = c.getColumnIndex(COL_NAME);
            int iRole = c.getColumnIndex(COL_SKILL);
            int iWage = c.getColumnIndex(COL_DAILY_WAGE);

            while (c.moveToNext()) {
                long workerId = iId >= 0 ? c.getLong(iId) : -1L;
                String workerName = iName >= 0 ? c.getString(iName) : "";
                String role = iRole >= 0 ? c.getString(iRole) : "";
                String wageText = iWage >= 0 ? c.getString(iWage) : "";

                double dailyWage = parseAmount(wageText);
                double workedDays = workedDaysMap.containsKey(workerId) ? workedDaysMap.get(workerId) : 0d;
                double grossAmount = dailyWage * workedDays;
                double paidAmount = paidMap.containsKey(workerId) ? paidMap.get(workerId) : 0d;
                double pendingAmount = Math.max(0d, grossAmount - paidAmount);

                summaries.add(new WorkerPaymentSummary(
                        workerId,
                        workerName,
                        role,
                        dailyWage,
                        workedDays,
                        grossAmount,
                        paidAmount,
                        pendingAmount
                ));
            }
            c.close();
        }

        db.close();
        return summaries;
    }

    /**
     * Returns attendance counts for one worker in a given month.
     * Array order: [presentCount, halfDayCount, absentCount].
     */
    public int[] getWorkerAttendanceCountsForMonth(long workerId, int year, int monthZeroBased) {
        int presentCount = 0;
        int halfDayCount = 0;
        int absentCount = 0;
        if (workerId <= 0L) {
            return new int[]{0, 0, 0};
        }

        SQLiteDatabase db = getReadableDatabase();
        String monthPrefix = String.format(Locale.US, "%04d-%02d-%%", year, monthZeroBased + 1);
        String sql = "SELECT " + COL_ATTENDANCE_STATUS + ", COUNT(1) "
                + "FROM " + TABLE_ATTENDANCE + " "
                + "WHERE " + COL_ATTENDANCE_WORKER_ID + "=? "
                + "AND " + COL_ATTENDANCE_DATE + " LIKE ? "
                + "AND " + COL_ATTENDANCE_LOCKED + "=1 "
                + "GROUP BY " + COL_ATTENDANCE_STATUS;
        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(workerId), monthPrefix});
        if (c != null) {
            while (c.moveToNext()) {
                int status = c.getInt(0);
                int count = c.getInt(1);
                if (status == AttendanceStaffItem.STATUS_PRESENT) {
                    presentCount = count;
                } else if (status == AttendanceStaffItem.STATUS_HALF_DAY) {
                    halfDayCount = count;
                } else if (status == AttendanceStaffItem.STATUS_ABSENT) {
                    absentCount = count;
                }
            }
            c.close();
        }
        db.close();

        return new int[]{presentCount, halfDayCount, absentCount};
    }

    /**
     * Returns overall attendance percentage for the given month across all workers.
     * present=1.0, half-day=0.5, absent=0.0
     */
    public float getAttendancePercentageForMonth(int year, int monthZeroBased) {
        SQLiteDatabase db = getReadableDatabase();
        String monthPrefix = String.format(Locale.US, "%04d-%02d-%%", year, monthZeroBased + 1);
        String sql = "SELECT "
                + "SUM(CASE " + COL_ATTENDANCE_STATUS
                + " WHEN " + AttendanceStaffItem.STATUS_PRESENT + " THEN 1.0"
                + " WHEN " + AttendanceStaffItem.STATUS_HALF_DAY + " THEN 0.5"
                + " ELSE 0.0 END) AS present_equivalent, "
                + "COUNT(1) AS total_entries "
                + "FROM " + TABLE_ATTENDANCE + " "
                + "WHERE " + COL_ATTENDANCE_DATE + " LIKE ? "
                + "AND " + COL_ATTENDANCE_LOCKED + "=1";

        Cursor c = db.rawQuery(sql, new String[]{monthPrefix});
        double presentEquivalent = 0d;
        int totalEntries = 0;
        if (c != null) {
            if (c.moveToFirst()) {
                presentEquivalent = c.isNull(0) ? 0d : c.getDouble(0);
                totalEntries = c.getInt(1);
            }
            c.close();
        }
        db.close();

        if (totalEntries <= 0) {
            return 0f;
        }
        return (float) ((presentEquivalent * 100d) / totalEntries);
    }

    private Map<Long, Double> readWorkedDaysMap(SQLiteDatabase db, String monthPrefixLike) {
        Map<Long, Double> map = new HashMap<>();
        String sql = "SELECT " + COL_ATTENDANCE_WORKER_ID + ", "
                + "SUM(CASE " + COL_ATTENDANCE_STATUS
                + " WHEN " + AttendanceStaffItem.STATUS_PRESENT + " THEN 1.0"
                + " WHEN " + AttendanceStaffItem.STATUS_HALF_DAY + " THEN 0.5"
                + " ELSE 0.0 END) AS worked_days "
                + "FROM " + TABLE_ATTENDANCE + " "
                + "WHERE " + COL_ATTENDANCE_DATE + " LIKE ? AND " + COL_ATTENDANCE_LOCKED + "=1 "
                + "GROUP BY " + COL_ATTENDANCE_WORKER_ID;
        Cursor c = db.rawQuery(sql, new String[]{ monthPrefixLike });
        if (c != null) {
            while (c.moveToNext()) {
                long workerId = c.getLong(0);
                double workedDays = c.getDouble(1);
                map.put(workerId, workedDays);
            }
            c.close();
        }
        return map;
    }

    private Map<Long, Double> readPaidAmountMap(SQLiteDatabase db, String monthPrefixLike) {
        Map<Long, Double> map = new HashMap<>();
        String sql = "SELECT " + COL_ADVANCE_WORKER_ID + ", SUM(" + COL_ADVANCE_AMOUNT + ") AS paid_amount "
                + "FROM " + TABLE_ADVANCES + " "
                + "WHERE " + COL_ADVANCE_DATE + " LIKE ? "
                + "GROUP BY " + COL_ADVANCE_WORKER_ID;
        Cursor c = db.rawQuery(sql, new String[]{ monthPrefixLike });
        if (c != null) {
            while (c.moveToNext()) {
                long workerId = c.getLong(0);
                double amount = c.getDouble(1);
                map.put(workerId, amount);
            }
            c.close();
        }
        return map;
    }

    private double parseAmount(String value) {
        if (value == null) return 0d;
        String clean = value.replaceAll("[^0-9.]", "");
        if (clean.trim().isEmpty()) return 0d;
        try {
            return Double.parseDouble(clean);
        } catch (Exception ignored) {
            return 0d;
        }
    }
}
