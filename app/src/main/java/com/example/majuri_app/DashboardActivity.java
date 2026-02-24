package com.example.majuri_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        updateWelcomeName();
        updateTotalWorkersCount();

        findViewById(R.id.quickActionAddWorker).setOnClickListener(v -> {
            startActivity(new Intent(this, AddWorkerActivity.class));
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_dashboard) {
                    return true;
                }
                if (id == R.id.nav_workers) {
                    startActivity(new Intent(this, WorkersListActivity.class));
                    finish();
                    return true;
                }
                if (id == R.id.nav_payments) {
                    startActivity(new Intent(this, PaymentsActivity.class));
                    finish();
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
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateWelcomeName();
        updateTotalWorkersCount();
    }

    private void updateWelcomeName() {
        SessionManager session = new SessionManager(this);
        String name = session.getLoggedInUserName();
        String display = name == null || name.trim().isEmpty()
                ? getString(R.string.welcome_admin)
                : getString(R.string.welcome_builder_format, name.trim());
        TextView tvWelcome = findViewById(R.id.tvWelcomeName);
        if (tvWelcome != null) tvWelcome.setText(display);
    }

    private void updateTotalWorkersCount() {
        WorkerDbHelper dbHelper = new WorkerDbHelper(this);
        int count = dbHelper.getWorkersCount();
        dbHelper.close();
        TextView tvCount = findViewById(R.id.tvTotalWorkersCount);
        if (tvCount != null) {
            tvCount.setText(String.valueOf(count));
        }
    }
}
