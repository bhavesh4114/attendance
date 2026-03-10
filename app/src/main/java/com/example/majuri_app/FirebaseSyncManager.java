package com.example.majuri_app;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.TimeUnit;

public final class FirebaseSyncManager {
    private static final String PERIODIC_WORK_NAME = "attendance_sync_periodic";
    private static final String ONE_TIME_WORK_NAME = "attendance_sync_one_time";
    private static final String DATABASE_URL =
            "https://labourattendanceapp-c4de5-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private FirebaseSyncManager() {
    }

    public static void enqueueAttendanceSync(
            Context context,
            long workerId,
            String attendanceDate,
            int status
    ) {
        enqueueAttendanceSync(context, workerId, attendanceDate, status, null);
    }

    public static void enqueueAttendanceSync(
            Context context,
            long workerId,
            String attendanceDate,
            int status,
            @Nullable WorkerDbHelper.DutyProof proof
    ) {
        if (context == null) return;

        WorkerDbHelper dbHelper = new WorkerDbHelper(context);
        try {
            WorkerDbHelper.DutyProof resolved = proof != null ? proof
                    : dbHelper.getDutyProofForDate(workerId, attendanceDate);
            dbHelper.upsertAttendanceSyncQueue(workerId, attendanceDate, status, resolved);
        } finally {
            dbHelper.close();
        }

        scheduleSyncWork(context);
    }

    public static void scheduleSyncWork(Context context) {
        if (context == null) return;
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest periodicRequest =
                new PeriodicWorkRequest.Builder(AttendanceSyncWorker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
        );

        OneTimeWorkRequest oneTimeRequest =
                new OneTimeWorkRequest.Builder(AttendanceSyncWorker.class)
                        .setConstraints(constraints)
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
        );
    }

    public static DatabaseReference getAttendanceRoot() {
        return FirebaseDatabase.getInstance(DATABASE_URL).getReference("attendance");
    }

    public static void pushAttendance(WorkerDbHelper.AttendanceSyncRecord record) throws Exception {
        if (record == null) return;
        DatabaseReference root = getAttendanceRoot();
        Tasks.await(
                root.child(String.valueOf(record.workerId))
                        .child(record.attendanceDate)
                        .setValue(record.toMap())
        );
    }
}
