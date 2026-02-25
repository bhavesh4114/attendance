package com.example.majuri_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AttendanceManagementActivity extends AppCompatActivity {

    private TextView tvMonthYear;
    private TextView summaryPresent;
    private TextView summaryAbsent;
    private TextView summaryHalfDay;
    private Calendar calendar;
    private AttendanceManagementAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_management);

        calendar = Calendar.getInstance();
        tvMonthYear = findViewById(R.id.tvMonthYear);
        summaryPresent = findViewById(R.id.summaryPresent);
        summaryAbsent = findViewById(R.id.summaryAbsent);
        summaryHalfDay = findViewById(R.id.summaryHalfDay);

        updateMonthLabel();
        updateWeekDates();
        setupMonthArrows();

        adapter = new AttendanceManagementAdapter();
        adapter.setItems(getStaffFromBackend());
        adapter.setOnSummaryChangedListener((present, halfDay, absent) -> {
            summaryPresent.setText(String.valueOf(present));
            summaryHalfDay.setText(String.valueOf(halfDay));
            summaryAbsent.setText(String.valueOf(absent));
        });
        RecyclerView recyclerStaff = findViewById(R.id.recyclerStaff);
        recyclerStaff.setLayoutManager(new LinearLayoutManager(this));
        recyclerStaff.setAdapter(adapter);
        recyclerStaff.setNestedScrollingEnabled(false);

        findViewById(R.id.btnSearch).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.calendar), Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnSaveAttendance).setOnClickListener(v -> {
            Toast.makeText(this, R.string.save_attendance, Toast.LENGTH_SHORT).show();
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_user_attendance);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_user_home) {
                startActivity(new Intent(this, UserDashboardActivity.class));
                finish();
                return true;
            }
            if (id == R.id.nav_user_attendance) {
                return true;
            }
            if (id == R.id.nav_user_payslips) {
                Toast.makeText(this, getString(R.string.user_dashboard_payslips), Toast.LENGTH_SHORT).show();
                return true;
            }
            if (id == R.id.nav_user_profile) {
                Toast.makeText(this, getString(R.string.nav_profile), Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    private void setupMonthArrows() {
        findViewById(R.id.btnMonthPrev).setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, -1);
            updateMonthLabel();
            updateWeekDates();
        });
        findViewById(R.id.btnMonthNext).setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, 1);
            updateMonthLabel();
            updateWeekDates();
        });
    }

    private void updateMonthLabel() {
        String month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US);
        int year = calendar.get(Calendar.YEAR);
        tvMonthYear.setText((month != null ? month.toUpperCase(Locale.US) : "") + " " + year);
    }

    private void updateWeekDates() {
        // Show week containing the 5th as example (Thu 5 selected)
        int[] dates = {2, 3, 4, 5, 6, 7, 8};
        int[] ids = {R.id.dateMon, R.id.dateTue, R.id.dateWed, R.id.dateThu, R.id.dateFri, R.id.dateSat, R.id.dateSun};
        for (int i = 0; i < ids.length; i++) {
            ((TextView) findViewById(ids[i])).setText(String.valueOf(dates[i]));
        }
    }

    private List<AttendanceStaffItem> getStaffFromBackend() {
        WorkerDbHelper dbHelper = new WorkerDbHelper(this);
        List<WorkerListItem> workers = dbHelper.getAllWorkers();
        dbHelper.close();

        List<AttendanceStaffItem> list = new java.util.ArrayList<>();
        for (WorkerListItem worker : workers) {
            String workerCode = "#WR-" + Math.max(worker.getId(), 0L);
            list.add(new AttendanceStaffItem(
                    worker.getName(),
                    worker.getRole(),
                    workerCode,
                    AttendanceStaffItem.STATUS_PRESENT
            ));
        }

        TextView tvTotalWorkers = findViewById(R.id.tvTotalWorkers);
        if (tvTotalWorkers != null) {
            tvTotalWorkers.setText("Total: " + list.size() + " Workers");
        }
        return list;
    }
}
