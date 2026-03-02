package com.example.majuri_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AttendanceManagementActivity extends AppCompatActivity {
    public static final String EXTRA_FORCE_USER_FLOW = "extra_force_user_flow";

    private TextView tvMonthYear;
    private TextView summaryPresent;
    private TextView summaryAbsent;
    private TextView summaryHalfDay;
    private TextView tvTotalWorkers;
    private Calendar calendar;
    private AttendanceManagementAdapter adapter;
    private SessionManager sessionManager;
    private boolean forceUserFlow;
    private final SimpleDateFormat dateKeyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_management);

        sessionManager = new SessionManager(this);
        forceUserFlow = getIntent() != null && getIntent().getBooleanExtra(EXTRA_FORCE_USER_FLOW, false);
        calendar = Calendar.getInstance();
        tvMonthYear = findViewById(R.id.tvMonthYear);
        summaryPresent = findViewById(R.id.summaryPresent);
        summaryAbsent = findViewById(R.id.summaryAbsent);
        summaryHalfDay = findViewById(R.id.summaryHalfDay);
        tvTotalWorkers = findViewById(R.id.tvTotalWorkers);

        updateMonthLabel();
        updateWeekDates();
        setupMonthArrows();

        adapter = new AttendanceManagementAdapter();
        adapter.setOnSummaryChangedListener((present, halfDay, absent) -> {
            summaryPresent.setText(String.valueOf(present));
            summaryHalfDay.setText(String.valueOf(halfDay));
            summaryAbsent.setText(String.valueOf(absent));
        });

        RecyclerView recyclerStaff = findViewById(R.id.recyclerStaff);
        recyclerStaff.setLayoutManager(new LinearLayoutManager(this));
        recyclerStaff.setAdapter(adapter);
        recyclerStaff.setNestedScrollingEnabled(false);

        loadAttendanceForSelectedDate();

        findViewById(R.id.btnSearch).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.calendar), Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnSaveAttendance).setOnClickListener(v -> saveAttendanceForSelectedDate());

        setupBottomNav();
    }

    @Override
    public void onBackPressed() {
        navigateToHomeByRole();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAttendanceForSelectedDate();
    }

    private void setupMonthArrows() {
        findViewById(R.id.btnMonthPrev).setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, -1);
            updateMonthLabel();
            updateWeekDates();
            loadAttendanceForSelectedDate();
        });
        findViewById(R.id.btnMonthNext).setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, 1);
            updateMonthLabel();
            updateWeekDates();
            loadAttendanceForSelectedDate();
        });
    }

    private void updateMonthLabel() {
        String month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US);
        int year = calendar.get(Calendar.YEAR);
        tvMonthYear.setText((month != null ? month.toUpperCase(Locale.US) : "") + " " + year);
    }

    private void updateWeekDates() {
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        int startDay = Math.max(1, Math.min(day - 3, Math.max(1, maxDays - 6)));

        int[] ids = {R.id.dateMon, R.id.dateTue, R.id.dateWed, R.id.dateThu, R.id.dateFri, R.id.dateSat, R.id.dateSun};
        for (int i = 0; i < ids.length; i++) {
            int dateNumber = startDay + i;
            ((TextView) findViewById(ids[i])).setText(String.valueOf(dateNumber));
        }
    }

    private String getSelectedDateKey() {
        return dateKeyFormat.format(calendar.getTime());
    }

    private void loadAttendanceForSelectedDate() {
        String attendanceDate = getSelectedDateKey();

        WorkerDbHelper dbHelper = new WorkerDbHelper(this);
        List<WorkerListItem> workers = dbHelper.getAllWorkers();
        Map<Long, Integer> savedStatus = dbHelper.getAttendanceStatusByDate(attendanceDate);
        boolean locked = dbHelper.isAttendanceLockedForDate(attendanceDate);
        dbHelper.close();

        List<AttendanceStaffItem> list = new ArrayList<>();
        for (WorkerListItem worker : workers) {
            int status = savedStatus.containsKey(worker.getId())
                    ? savedStatus.get(worker.getId())
                    : AttendanceStaffItem.STATUS_PRESENT;
            String workerCode = "#WR-" + Math.max(worker.getId(), 0L);
            list.add(new AttendanceStaffItem(
                    worker.getId(),
                    worker.getName(),
                    worker.getRole(),
                    workerCode,
                    status
            ));
        }

        adapter.setItems(list);
        adapter.setEditable(!locked);

        if (tvTotalWorkers != null) {
            tvTotalWorkers.setText(getString(R.string.total_workers_with_count, list.size()));
        }

        updateSaveButtonState(locked);
    }

    private void saveAttendanceForSelectedDate() {
        String attendanceDate = getSelectedDateKey();

        WorkerDbHelper dbHelper = new WorkerDbHelper(this);
        if (dbHelper.isAttendanceLockedForDate(attendanceDate)) {
            dbHelper.close();
            adapter.setEditable(false);
            updateSaveButtonState(true);
            Toast.makeText(this, R.string.attendance_already_locked, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean saved = dbHelper.saveAttendanceForDate(attendanceDate, adapter.getItems());
        dbHelper.close();

        if (saved) {
            adapter.setEditable(false);
            updateSaveButtonState(true);
            Toast.makeText(this, R.string.attendance_saved_locked, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.attendance_save_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSaveButtonState(boolean locked) {
        View btnSaveAttendance = findViewById(R.id.btnSaveAttendance);
        btnSaveAttendance.setEnabled(!locked);
        btnSaveAttendance.setAlpha(locked ? 0.6f : 1f);
    }

    private void navigateToHomeByRole() {
        boolean isAdmin = sessionManager != null && sessionManager.isAdmin();
        Class<?> home = forceUserFlow ? UserDashboardActivity.class : (isAdmin ? DashboardActivity.class : UserDashboardActivity.class);
        Intent target = new Intent(this, home);
        target.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(target);
        finish();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        boolean useUserNav = forceUserFlow || (sessionManager != null && !sessionManager.isAdmin());

        bottomNav.getMenu().clear();
        if (useUserNav) {
            bottomNav.inflateMenu(R.menu.menu_user_dashboard_nav);
            bottomNav.setSelectedItemId(R.id.nav_user_attendance);
            bottomNav.setOnItemSelectedListener(item -> onUserNavItemSelected(item.getItemId()));
        } else {
            bottomNav.inflateMenu(R.menu.menu_dashboard_bottom_nav);
            bottomNav.setSelectedItemId(R.id.nav_attendance);
            bottomNav.setOnItemSelectedListener(item -> onAdminNavItemSelected(item.getItemId()));
        }
    }

    private boolean onUserNavItemSelected(int id) {
        if (id == R.id.nav_user_home) {
            navigateToHomeByRole();
            return true;
        }
        if (id == R.id.nav_user_attendance) {
            return true;
        }
        if (id == R.id.nav_user_workers) {
            Intent intent = new Intent(this, WorkersListActivity.class);
            intent.putExtra(WorkersListActivity.EXTRA_FORCE_USER_FLOW, true);
            startActivity(intent);
            finish();
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
    }

    private boolean onAdminNavItemSelected(int id) {
        if (id == R.id.nav_dashboard) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return true;
        }
        if (id == R.id.nav_workers) {
            startActivity(new Intent(this, WorkersListActivity.class));
            finish();
            return true;
        }
        if (id == R.id.nav_attendance) {
            return true;
        }
        if (id == R.id.nav_analytics) {
            startActivity(new Intent(this, ReportsActivity.class));
            finish();
            return true;
        }
        if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            finish();
            return true;
        }
        return false;
    }
}
