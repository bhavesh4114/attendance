package com.example.majuri_app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        installBackHandler();

        updateWelcomeName();
        updateTotalWorkersCount();
        bindQuickActions();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_dashboard) {
                    return true;
                }
                if (id == R.id.nav_workers) {
                    navigateTo(WorkersListActivity.class);
                    return true;
                }
                if (id == R.id.nav_attendance) {
                    navigateTo(AttendanceActivity.class);
                    return true;
                }
                if (id == R.id.nav_settings) {
                    navigateTo(SettingsActivity.class);
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

    private void installBackHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateBackToLogin();
            }
        });
    }

    private void navigateBackToLogin() {
        if (isTaskRoot()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
    }

    private void navigateTo(Class<? extends Activity> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void bindQuickActions() {
        bindClick(R.id.quickActionAddWorker, AddWorkerActivity.class);
        bindClick(R.id.cardViewAttendance, ReportsActivity.class); // Reports icon in Quick Actions
        bindClick(R.id.quickActionAnalytics, PaymentsActivity.class);
    }

    private void bindClick(int viewId, Class<? extends Activity> targetActivity) {
        android.view.View quickAction = findViewById(viewId);
        if (quickAction != null) {
            quickAction.setOnClickListener(v -> navigateTo(targetActivity));
        }
    }
}
