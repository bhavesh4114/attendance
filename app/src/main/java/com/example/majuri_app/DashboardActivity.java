package com.example.majuri_app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.PopupMenu;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;
import java.util.Locale;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {
    private static final String[] ATTENDANCE_RANGE_OPTIONS = {
            "7 Day", "1 Month", "3 Month", "6 Month", "12 Month"
    };
    private static final int CHART_BAR_COUNT = 7;
    private static final int[] TREND_DAY_LABEL_IDS = {
            R.id.tvTrendDay1, R.id.tvTrendDay2, R.id.tvTrendDay3, R.id.tvTrendDay4,
            R.id.tvTrendDay5, R.id.tvTrendDay6, R.id.tvTrendDay7
    };

    private TextView tvAttendanceRange;
    private LinearLayout chartBarsContainer;
    private int selectedAttendanceRangeIndex = 0;
    private final SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat shortWeekDayFormat = new SimpleDateFormat("E", Locale.US);
    private final SimpleDateFormat shortMonthDayFormat = new SimpleDateFormat("d/M", Locale.US);

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
        refreshAttendanceTrendChart();
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
        float attendancePercent = dbHelper.getAttendancePercentageForMonth(year, month);
        double pendingTotal = dbHelper.getTotalPendingWorkerPaymentAmount();
        double walletBalance = dbHelper.getAvailableWalletBalance();
        dbHelper.close();

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
        chartBarsContainer = findViewById(R.id.chartBarsContainer);
        View selector = findViewById(R.id.attendanceRangeSelector);

        if (tvAttendanceRange != null) {
            tvAttendanceRange.setText(ATTENDANCE_RANGE_OPTIONS[0]);
        }
        if (selector != null) {
            selector.setOnClickListener(this::showAttendanceRangeMenu);
        }

        refreshAttendanceTrendChart();
    }

    private void showAttendanceRangeMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        for (int i = 0; i < ATTENDANCE_RANGE_OPTIONS.length; i++) {
            popupMenu.getMenu().add(0, i, i, ATTENDANCE_RANGE_OPTIONS[i]);
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            selectedAttendanceRangeIndex = item.getItemId();
            if (tvAttendanceRange != null) tvAttendanceRange.setText(item.getTitle());
            refreshAttendanceTrendChart();
            return true;
        });
        popupMenu.show();
    }

    private void refreshAttendanceTrendChart() {
        List<Calendar> dates = buildRangeDates(getSelectedRangeDays());
        if (dates.isEmpty()) return;

        String fromDate = isoDateFormat.format(dates.get(0).getTime());
        String toDate = isoDateFormat.format(dates.get(dates.size() - 1).getTime());

        WorkerDbHelper dbHelper = new WorkerDbHelper(this);
        Map<String, Float> datePercentMap = dbHelper.getAttendancePercentageByDateRange(fromDate, toDate);
        dbHelper.close();

        float[] bucketValues = buildBucketValues(dates, datePercentMap, CHART_BAR_COUNT);
        renderAttendanceTrendChart(bucketValues, dates);
    }

    private int getSelectedRangeDays() {
        switch (selectedAttendanceRangeIndex) {
            case 1: return 30;
            case 2: return 90;
            case 3: return 180;
            case 4: return 365;
            default: return 7;
        }
    }

    private List<Calendar> buildRangeDates(int totalDays) {
        List<Calendar> dates = new ArrayList<>();
        int safeDays = Math.max(1, totalDays);
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar start = (Calendar) today.clone();
        start.add(Calendar.DAY_OF_MONTH, -(safeDays - 1));

        for (int i = 0; i < safeDays; i++) {
            Calendar current = (Calendar) start.clone();
            current.add(Calendar.DAY_OF_MONTH, i);
            dates.add(current);
        }
        return dates;
    }

    private float[] buildBucketValues(List<Calendar> dates, Map<String, Float> datePercentMap, int bucketCount) {
        float[] values = new float[Math.max(1, bucketCount)];
        if (dates == null || dates.isEmpty()) return values;

        int totalDays = dates.size();
        for (int i = 0; i < values.length; i++) {
            int start = (i * totalDays) / values.length;
            int endExclusive = ((i + 1) * totalDays) / values.length;
            if (endExclusive <= start) endExclusive = Math.min(totalDays, start + 1);

            float sum = 0f;
            int count = 0;
            for (int dayIndex = start; dayIndex < endExclusive; dayIndex++) {
                String dateKey = isoDateFormat.format(dates.get(dayIndex).getTime());
                Float value = datePercentMap.get(dateKey);
                if (value != null) {
                    sum += Math.max(0f, Math.min(100f, value));
                    count++;
                }
            }
            values[i] = count > 0 ? (sum / count) : 0f;
        }

        return values;
    }

    private void renderAttendanceTrendChart(float[] values, List<Calendar> rangeDates) {
        if (chartBarsContainer == null) return;

        chartBarsContainer.removeAllViews();
        float maxValue = 1f;
        for (float value : values) {
            maxValue = Math.max(maxValue, value);
        }

        for (int i = 0; i < values.length; i++) {
            float ratio = values[i] / maxValue;
            int barHeightDp = 32 + Math.round(92f * Math.max(0f, Math.min(1f, ratio)));

            View bar = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(barHeightDp), 1f);
            params.leftMargin = dp(5);
            params.rightMargin = dp(5);
            bar.setLayoutParams(params);
            bar.setBackgroundResource(R.drawable.bg_chart_bar_primary);
            chartBarsContainer.addView(bar);
        }

        updateTrendDayLabels(rangeDates);
    }

    private void updateTrendDayLabels(List<Calendar> rangeDates) {
        if (rangeDates == null || rangeDates.isEmpty()) return;
        boolean weeklyRange = getSelectedRangeDays() == 7;
        int total = rangeDates.size();

        for (int i = 0; i < TREND_DAY_LABEL_IDS.length; i++) {
            TextView dayLabel = findViewById(TREND_DAY_LABEL_IDS[i]);
            if (dayLabel == null) continue;

            int index = ((i + 1) * total) / TREND_DAY_LABEL_IDS.length - 1;
            if (index < 0) index = 0;
            if (index >= total) index = total - 1;

            Date date = rangeDates.get(index).getTime();
            String raw = weeklyRange ? shortWeekDayFormat.format(date) : shortMonthDayFormat.format(date);
            if (raw == null || raw.trim().isEmpty()) raw = "-";
            String label = weeklyRange ? raw.substring(0, 1).toUpperCase(Locale.US) : raw;
            dayLabel.setText(label);
        }
    }

    private int dp(int valueDp) {
        return Math.round(valueDp * getResources().getDisplayMetrics().density);
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
