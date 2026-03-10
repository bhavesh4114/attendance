package com.example.majuri_app;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;


import java.util.List;

public class AttendanceSyncWorker extends Worker {
    private static final int BATCH_SIZE = 100;

    public AttendanceSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        WorkerDbHelper dbHelper = new WorkerDbHelper(getApplicationContext());
        List<WorkerDbHelper.AttendanceSyncRecord> records =
                dbHelper.getPendingAttendanceSync(BATCH_SIZE);

        if (records.isEmpty()) {
            dbHelper.close();
            return Result.success();
        }

        boolean failed = false;
        try {
            for (WorkerDbHelper.AttendanceSyncRecord record : records) {
                if (record == null) continue;
                try {
                    FirebaseSyncManager.pushAttendance(record);
                    dbHelper.deleteAttendanceSyncRecord(record.workerId, record.attendanceDate);
                } catch (Exception e) {
                    failed = true;
                    break;
                }
            }
        } finally {
            dbHelper.close();
        }

        return failed ? Result.retry() : Result.success();
    }
}
