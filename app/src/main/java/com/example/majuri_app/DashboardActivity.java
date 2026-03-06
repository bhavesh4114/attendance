package com.example.majuri_app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.PopupMenu;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.text.NumberFormat;

public class DashboardActivity extends AppCompatActivity {
    private static final String[] ATTENDANCE_RANGE_OPTIONS = {
            "7 Day", "1 Month", "3 Month", "6 Month", "12 Month"
    };

    private TextView tvAttendanceRange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contractor_dashboard);
        installBackHandler();
        bindAttendanceRangeSelector();

        updateWelcomeName();
        updateTotalWorkersCount();
        updateDashboardStats();
        bindQuickActions();
        NotificationStore.seedIfEmpty(this);
        findViewById(R.id.btnNotifications).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationActivity.class)));
        updateNotificationDot();

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
        updateDashboardStats();
        updateNotificationDot();
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

    private void updateDashboardStats() {
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH);

        WorkerDbHelper dbHelper = new WorkerDbHelper(this);
        List<WorkerPaymentSummary> summaries = dbHelper.getWorkerPaymentSummariesForMonth(year, month);
        float attendancePercent = dbHelper.getAttendancePercentageForMonth(year, month);
        double walletBalance = dbHelper.getAvailableWalletBalance();
        dbHelper.close();

        double pendingTotal = 0d;
        for (WorkerPaymentSummary summary : summaries) {
            if (summary == null) continue;
            pendingTotal += summary.getPendingAmount();
        }

        TextView tvAttendanceValue = findViewById(R.id.tvAttendanceValue);
        if (tvAttendanceValue != null) {
            tvAttendanceValue.setText(String.format(Locale.US, "%d%%", Math.round(attendancePercent)));
        }

        TextView tvPendingPayValue = findViewById(R.id.tvPendingPayValue);
        if (tvPendingPayValue != null) {
            tvPendingPayValue.setText(formatCompactCurrencyInr(pendingTotal));
        }

        TextView tvMonthlyCostValue = findViewById(R.id.tvMonthlyCostValue);
        if (tvMonthlyCostValue != null) {
            tvMonthlyCostValue.setText(formatExactCurrencyInr(walletBalance));
        }
    }

    private void bindAttendanceRangeSelector() {
        tvAttendanceRange = findViewById(R.id.tvAttendanceRange);
        View selector = findViewById(R.id.attendanceRangeSelector);

        if (tvAttendanceRange != null) {
            tvAttendanceRange.setText(ATTENDANCE_RANGE_OPTIONS[0]);
        }
        if (selector != null) {
            selector.setOnClickListener(this::showAttendanceRangeMenu);
        }
    }

    private void showAttendanceRangeMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        for (int i = 0; i < ATTENDANCE_RANGE_OPTIONS.length; i++) {
            popupMenu.getMenu().add(0, i, i, ATTENDANCE_RANGE_OPTIONS[i]);
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            if (tvAttendanceRange != null) {
                tvAttendanceRange.setText(item.getTitle());
            }
            return true;
        });
        popupMenu.show();
    }

    private String formatCompactCurrencyInr(double amount) {
        double safeAmount = Math.max(0d, amount);
        if (safeAmount >= 100000d) {
            return "\u20B9" + trimTrailingZero(String.format(Locale.US, "%.1f", safeAmount / 100000d)) + "L";
        }
        if (safeAmount >= 1000d) {
            return "\u20B9" + trimTrailingZero(String.format(Locale.US, "%.1f", safeAmount / 1000d)) + "K";
        }
        return "\u20B9" + String.valueOf(Math.round(safeAmount));
    }

    private String formatExactCurrencyInr(double amount) {
        double safeAmount = Math.max(0d, amount);
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("en", "IN"));
        formatter.setMaximumFractionDigits(0);
        formatter.setMinimumFractionDigits(0);
        return "\u20B9" + formatter.format(Math.round(safeAmount));
    }

    private String trimTrailingZero(String value) {
        if (value == null) return "0";
        if (value.endsWith(".0")) {
            return value.substring(0, value.length() - 2);
        }
        return value;
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

    private void updateNotificationDot() {
        View dot = findViewById(R.id.notificationDot);
        if (dot != null) {
            dot.setVisibility(NotificationStore.hasUnread(this) ? View.VISIBLE : View.GONE);
        }
    }

    private void bindClick(int viewId, Class<? extends Activity> targetActivity) {
        android.view.View quickAction = findViewById(viewId);
        if (quickAction != null) {
            quickAction.setOnClickListener(v -> navigateTo(targetActivity));
        }
    }
}
