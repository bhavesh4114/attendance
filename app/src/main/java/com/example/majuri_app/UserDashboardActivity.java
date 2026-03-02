package com.example.majuri_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * User (worker) dashboard – separate from admin DashboardActivity.
 * Uses activity_user_dashboard.xml.
 */
public class UserDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);
        bindLoggedInUserName();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_user_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_user_home) {
                return true;
            }
            if (id == R.id.nav_user_attendance) {
                openAttendanceScreen();
                return true;
            }
            if (id == R.id.nav_user_workers) {
                openWorkersScreen();
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

        findViewById(R.id.btnNotifications).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.menu), Toast.LENGTH_SHORT).show());

        findViewById(R.id.cardViewWorkers).setOnClickListener(v ->
                openWorkersScreen());
        findViewById(R.id.cardViewAttendance).setOnClickListener(v ->
                openAttendanceScreen());
        findViewById(R.id.cardViewPayments).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.nav_payments), Toast.LENGTH_SHORT).show());
        findViewById(R.id.cardViewReports).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.nav_reports), Toast.LENGTH_SHORT).show());
    }

    private void openAttendanceScreen() {
        Intent intent = new Intent(this, AttendanceManagementActivity.class);
        intent.putExtra(AttendanceManagementActivity.EXTRA_FORCE_USER_FLOW, true);
        startActivity(intent);
        finish();
    }

    private void openWorkersScreen() {
        Intent intent = new Intent(this, WorkersListActivity.class);
        intent.putExtra(WorkersListActivity.EXTRA_FORCE_USER_FLOW, true);
        startActivity(intent);
        finish();
    }

    private void bindLoggedInUserName() {
        TextView tvUserName = findViewById(R.id.tvUserName);
        if (tvUserName == null) return;

        SessionManager sessionManager = new SessionManager(this);
        String userName = sessionManager.getLoggedInUserName();
        if (userName == null || userName.trim().isEmpty()) {
            userName = "Builder";
        }
        tvUserName.setText(userName.trim());
    }
}
