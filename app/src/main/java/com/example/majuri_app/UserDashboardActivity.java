package com.example.majuri_app;

import android.content.Intent;
import android.os.Bundle;
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

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_user_home) {
                return true;
            }
            if (id == R.id.nav_user_attendance) {
                Toast.makeText(this, getString(R.string.nav_attendance), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, getString(R.string.nav_workers), Toast.LENGTH_SHORT).show());
        findViewById(R.id.cardViewAttendance).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.nav_attendance), Toast.LENGTH_SHORT).show());
        findViewById(R.id.cardViewPayments).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.nav_payments), Toast.LENGTH_SHORT).show());
        findViewById(R.id.cardViewReports).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.nav_reports), Toast.LENGTH_SHORT).show());
    }
}
