package com.example.majuri_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * User (worker) dashboard - separate from admin DashboardActivity.
 * Uses activity_user_dashboard.xml.
 */
public class UserDashboardActivity extends AppCompatActivity {
    private View notificationDot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_builder_dashboard);
        NotificationStore.seedIfEmpty(this);
        bindLoggedInUserName();
        installBackHandler();
        notificationDot = findViewById(R.id.notificationDot);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_user_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_user_home) {
                return true;
            }
            if (id == R.id.nav_user_workers) {
                openWorkersScreen();
                return true;
            }
            if (id == R.id.nav_user_payslips) {
                openFundsScreen();
                return true;
            }
            if (id == R.id.nav_user_profile) {
                startActivity(new Intent(this, BuilderSettingsActivity.class));
                return true;
            }
            return false;
        });

        findViewById(R.id.btnNotifications).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationActivity.class)));

        findViewById(R.id.cardViewWorkers).setOnClickListener(v -> openWorkersScreen());
        findViewById(R.id.cardViewAttendance).setOnClickListener(v -> openAttendanceScreen());

        findViewById(R.id.cardViewPayments).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.nav_payments), Toast.LENGTH_SHORT).show());
        bindClick(R.id.cardViewReports, ReportsActivity.class);
        bindSiteProgressPercent();
        updateDashboardData();
        updateNotificationDot();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDashboardData();
        updateNotificationDot();
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

    private void openFundsScreen() {
        startActivity(new Intent(this, FundsActivity.class));
        finish();
    }

    private void bindClick(int viewId, Class<?> targetActivity) {
        android.view.View view = findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(v -> startActivity(new Intent(this, targetActivity)));
        }
    }

    private void bindLoggedInUserName() {
        TextView tvUserName = findViewById(R.id.tvUserName);
        TextView tvUserDate = findViewById(R.id.tvUserDate);
        if (tvUserName == null) return;

        SessionManager sessionManager = new SessionManager(this);
        String loginId = sessionManager.getLoggedInMobile();
        String userName = sessionManager.getLoggedInUserName();

        BuilderDbHelper builderDbHelper = new BuilderDbHelper(this);
        String builderDisplay = builderDbHelper.getBuilderDisplayName(loginId);
        builderDbHelper.close();

        if (userName == null || userName.trim().isEmpty()) {
            userName = "Builder";
        }
        tvUserName.setText(userName.trim());

        if (tvUserDate != null) {
            String companyText = builderDisplay != null ? builderDisplay.trim() : "";
            if (companyText.isEmpty()) {
                companyText = "BUILDER PROFILE";
            }
            tvUserDate.setText(companyText.toUpperCase(Locale.US));
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

    private void updateNotificationDot() {
        if (notificationDot != null) {
            notificationDot.setVisibility(NotificationStore.hasUnread(this) ? View.VISIBLE : View.GONE);
        }
    }

    private void bindSiteProgressPercent() {
        ProgressBar progressBar = findViewById(R.id.progressSiteOverview);
        TextView tvPercent = findViewById(R.id.tvSiteProgressPercent);
        if (progressBar != null && tvPercent != null) {
            tvPercent.setText(progressBar.getProgress() + "%");
        }
    }

    private void updateDashboardData() {
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH);
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now.getTime());

        WorkerDbHelper dbHelper = new WorkerDbHelper(this);
        int totalWorkers = dbHelper.getWorkersCount();
        List<String> skillTypes = dbHelper.getAllSkillTypes();
        int activeSites = skillTypes.size();
        List<WorkerPaymentSummary> paymentSummaries = dbHelper.getWorkerPaymentSummariesForMonth(year, month);
        Map<Long, Integer> todayAttendance = dbHelper.getAttendanceStatusByDate(todayDate);
        int monthlyAttendancePercent = Math.round(dbHelper.getAttendancePercentageForMonth(year, month));
        int hourlyCount = dbHelper.getOngoingDutyWorkerCountForDate(todayDate);
        int otCount = dbHelper.getOvertimeWorkerCountForDate(todayDate);
        double walletBalance = dbHelper.getAvailableWalletBalance();
        dbHelper.close();

        BuilderDbHelper builderDbHelper = new BuilderDbHelper(this);
        List<String> businessNames = builderDbHelper.getAllBusinessNames();
        builderDbHelper.close();

        int fullDayCount = 0;
        int halfDayCount = 0;
        for (Integer status : todayAttendance.values()) {
            if (status == null) continue;
            if (status == AttendanceStaffItem.STATUS_PRESENT) {
                fullDayCount++;
            } else if (status == AttendanceStaffItem.STATUS_HALF_DAY) {
                halfDayCount++;
            }
        }

        double pendingPay = 0d;
        for (WorkerPaymentSummary summary : paymentSummaries) {
            if (summary == null) continue;
            pendingPay += summary.getPendingAmount();
        }
        double netPendingPay = Math.max(0d, pendingPay - walletBalance);

        int presentEquivalent = (int) Math.round(fullDayCount + (halfDayCount * 0.5d));

        setText(R.id.tvTotalWorkersValue, formatIndianNumber(totalWorkers));
        setText(R.id.tvTodayStatus, formatIndianNumber(presentEquivalent));
        setText(R.id.tvActiveSitesValue, formatIndianNumber(activeSites));
        setText(R.id.tvMyDues, formatCompactCurrencyInr(netPendingPay));
        setText(R.id.tvAttendanceDays, formatIndianNumber(fullDayCount));
        setText(R.id.tvHalfDayCount, formatIndianNumber(halfDayCount));
        setText(R.id.tvHourlyCount, formatIndianNumber(hourlyCount));
        setText(R.id.tvOtCount, formatIndianNumber(otCount));

        bindSiteOverviewFromDb(businessNames, skillTypes, monthlyAttendancePercent);
    }

    private void bindSiteOverviewFromDb(List<String> businessNames, List<String> skillTypes, int monthlyAttendancePercent) {
        List<String> safeBusinesses = businessNames != null ? businessNames : new ArrayList<>();
        List<String> safeSkills = skillTypes != null ? skillTypes : new ArrayList<>();

        String siteName = !safeBusinesses.isEmpty() ? safeBusinesses.get(0) : "Primary Site";
        String subtitle = !safeSkills.isEmpty()
                ? safeSkills.size() + " active categories"
                : "No skill categories added";

        setText(R.id.tvSiteOverviewTitle, siteName);
        setText(R.id.tvSiteOverviewSubtitle, subtitle);

        int safePercent = Math.max(0, Math.min(100, monthlyAttendancePercent));
        ProgressBar progressBar = findViewById(R.id.progressSiteOverview);
        TextView tvPercent = findViewById(R.id.tvSiteProgressPercent);
        if (progressBar != null) progressBar.setProgress(safePercent);
        if (tvPercent != null) tvPercent.setText(safePercent + "%");
    }

    private void setText(int viewId, String value) {
        TextView tv = findViewById(viewId);
        if (tv != null) {
            tv.setText(value);
        }
    }

    private String formatIndianNumber(int value) {
        return String.format(new Locale("en", "IN"), "%,d", Math.max(0, value));
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

    private String trimTrailingZero(String value) {
        if (value == null) return "0";
        if (value.endsWith(".0")) {
            return value.substring(0, value.length() - 2);
        }
        return value;
    }
}
