package com.example.majuri_app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PaymentsActivity extends AppCompatActivity {

    private static final int FILTER_ALL = 0;
    private static final int FILTER_OVERDUE = 1;
    private static final int FILTER_PENDING = 2;

    private PaymentQueueAdapter adapter;
    private final List<PaymentQueueItem> allItems = new ArrayList<>();
    private int currentFilter = FILTER_ALL;

    private EditText searchPayments;
    private TextView tvPendingTotal;
    private TextView tvPaidTotal;
    private TextView tvPaymentQueueTitle;
    private TextView btnMarkAllPaid;
    private View notificationDot;
    private Calendar selectedMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payments);

        selectedMonth = Calendar.getInstance();

        searchPayments = findViewById(R.id.searchPayments);
        tvPendingTotal = findViewById(R.id.tvPendingTotal);
        tvPaidTotal = findViewById(R.id.tvPaidTotal);
        tvPaymentQueueTitle = findViewById(R.id.tvPaymentQueueTitle);
        btnMarkAllPaid = findViewById(R.id.btnMarkAllPaid);
        notificationDot = findViewById(R.id.notificationDot);

        adapter = new PaymentQueueAdapter();
        adapter.setOnMarkPaidClickListener((item, position) -> markSinglePaid(item));

        RecyclerView recyclerPayments = findViewById(R.id.recyclerPayments);
        recyclerPayments.setLayoutManager(new LinearLayoutManager(this));
        recyclerPayments.setAdapter(adapter);
        recyclerPayments.setNestedScrollingEnabled(false);

        bindSearchAndFilters();

        NotificationStore.seedIfEmpty(this);
        findViewById(R.id.btnNotifications).setOnClickListener(v -> openNotificationScreen());
        findViewById(R.id.btnFilter).setOnClickListener(v ->
                Toast.makeText(this, R.string.filter, Toast.LENGTH_SHORT).show());
        btnMarkAllPaid.setOnClickListener(v -> markAllPaid());
        findViewById(R.id.cardTransactionHistory).setOnClickListener(v ->
                Toast.makeText(this, R.string.transaction_history, Toast.LENGTH_SHORT).show());

        loadPaymentData();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_dashboard);
        bottomNav.setOnItemSelectedListener(item -> onNavItemSelected(item.getItemId()));
        updateNotificationDot();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNotificationDot();
    }

    private void bindSearchAndFilters() {
        searchPayments.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilterAndSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        findViewById(R.id.chipAll).setOnClickListener(v -> {
            currentFilter = FILTER_ALL;
            applyFilterAndSearch();
        });

        findViewById(R.id.chipOverdue).setOnClickListener(v -> {
            currentFilter = FILTER_OVERDUE;
            applyFilterAndSearch();
        });

        findViewById(R.id.chipPending).setOnClickListener(v -> {
            currentFilter = FILTER_PENDING;
            applyFilterAndSearch();
        });
    }

    private void loadPaymentData() {
        WorkerDbHelper dbHelper = new WorkerDbHelper(this);
        List<WorkerPaymentSummary> summaries = dbHelper.getWorkerPaymentSummariesForMonth(
                selectedMonth.get(Calendar.YEAR),
                selectedMonth.get(Calendar.MONTH)
        );
        dbHelper.close();

        allItems.clear();

        double totalPending = 0d;
        double totalPaid = 0d;
        String dueDateLabel = getMonthEndDateLabel(selectedMonth);
        boolean overdue = isSelectedMonthPast(selectedMonth);

        for (WorkerPaymentSummary summary : summaries) {
            totalPending += summary.getPendingAmount();
            totalPaid += summary.getPaidAmount();

            String workerName = safeText(summary.getWorkerName(), "Worker");
            String role = safeText(summary.getRole(), "Worker");
            String details = role
                    + " - " + formatDays(summary.getWorkedDays()) + " " + getString(R.string.days_short)
                    + " - " + formatCurrency(summary.getDailyWage()) + "/" + getString(R.string.day_short);

            allItems.add(new PaymentQueueItem(
                    summary.getWorkerId(),
                    workerName,
                    details,
                    dueDateLabel,
                    summary.getPendingAmount(),
                    overdue && summary.getPendingAmount() > 0d
            ));
        }

        tvPendingTotal.setText(formatCurrency(totalPending));
        tvPaidTotal.setText(formatCurrency(totalPaid));

        applyFilterAndSearch();
    }

    private void applyFilterAndSearch() {
        String query = searchPayments.getText() != null
                ? searchPayments.getText().toString().trim().toLowerCase(Locale.US)
                : "";

        List<PaymentQueueItem> filtered = new ArrayList<>();
        for (PaymentQueueItem item : allItems) {
            if (currentFilter == FILTER_OVERDUE && !item.isOverdue()) {
                continue;
            }
            if (currentFilter == FILTER_PENDING && item.getPendingAmountValue() <= 0d) {
                continue;
            }

            if (!query.isEmpty()) {
                boolean nameMatch = item.getWorkerName() != null
                        && item.getWorkerName().toLowerCase(Locale.US).contains(query);
                boolean roleMatch = item.getRoleAndId() != null
                        && item.getRoleAndId().toLowerCase(Locale.US).contains(query);
                if (!nameMatch && !roleMatch) {
                    continue;
                }
            }

            filtered.add(item);
        }

        adapter.setItems(filtered);
        tvPaymentQueueTitle.setText(getString(R.string.payment_queue_count, filtered.size()));

        boolean hasPending = false;
        for (PaymentQueueItem item : filtered) {
            if (item.getPendingAmountValue() > 0d) {
                hasPending = true;
                break;
            }
        }
        btnMarkAllPaid.setEnabled(hasPending);
        btnMarkAllPaid.setAlpha(hasPending ? 1f : 0.6f);
    }

    private void markSinglePaid(PaymentQueueItem item) {
        if (item == null || item.getWorkerId() <= 0L || item.getPendingAmountValue() <= 0d) {
            Toast.makeText(this, R.string.no_pending_payments, Toast.LENGTH_SHORT).show();
            return;
        }

        WorkerDbHelper dbHelper = new WorkerDbHelper(this);
        double availableBalance = dbHelper.getAvailableWalletBalance();
        String walletFailureReason = getWalletPaymentFailureReason(item.getPendingAmountValue(), availableBalance);
        if (!walletFailureReason.isEmpty()) {
            dbHelper.close();
            showPaymentFailurePopup(walletFailureReason);
            return;
        }

        long rowId = dbHelper.recordPayment(
                item.getWorkerId(),
                item.getPendingAmountValue(),
                getTodayIsoDate(),
                "Cash",
                "Marked paid from payment queue"
        );
        dbHelper.close();

        if (rowId > 0L) {
            NotificationStore.pushNotification(
                    this,
                    "Payment Recorded",
                    item.getWorkerName() + " paid " + formatCurrency(item.getPendingAmountValue())
            );
            Toast.makeText(this,
                    getString(R.string.payment_recorded_amount, formatCurrency(item.getPendingAmountValue())),
                    Toast.LENGTH_SHORT).show();
            loadPaymentData();
            updateNotificationDot();
        } else {
            Toast.makeText(this, R.string.payment_record_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void markAllPaid() {
        int count = 0;
        WorkerDbHelper dbHelper = new WorkerDbHelper(this);
        double totalRequired = 0d;
        for (PaymentQueueItem item : allItems) {
            if (item == null || item.getWorkerId() <= 0L || item.getPendingAmountValue() <= 0d) continue;
            totalRequired += item.getPendingAmountValue();
        }

        double availableBalance = dbHelper.getAvailableWalletBalance();
        String walletFailureReason = getWalletPaymentFailureReason(totalRequired, availableBalance);
        if (!walletFailureReason.isEmpty()) {
            dbHelper.close();
            showPaymentFailurePopup(walletFailureReason);
            return;
        }

        for (PaymentQueueItem item : allItems) {
            if (item.getWorkerId() <= 0L || item.getPendingAmountValue() <= 0d) {
                continue;
            }
            long rowId = dbHelper.recordPayment(
                    item.getWorkerId(),
                    item.getPendingAmountValue(),
                    getTodayIsoDate(),
                    "Cash",
                    "Marked paid (all)"
            );
            if (rowId > 0L) {
                count++;
            }
        }
        dbHelper.close();

        if (count == 0) {
            Toast.makeText(this, R.string.no_pending_payments, Toast.LENGTH_SHORT).show();
            return;
        }

        NotificationStore.pushNotification(
                this,
                "Bulk Payment Update",
                count + " workers marked as paid."
        );
        Toast.makeText(this, getString(R.string.marked_paid_count, count), Toast.LENGTH_SHORT).show();
        loadPaymentData();
        updateNotificationDot();
    }

    private String getTodayIsoDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());
    }

    private String getMonthEndDateLabel(Calendar monthCalendar) {
        Calendar end = (Calendar) monthCalendar.clone();
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
        return new SimpleDateFormat("dd MMM, yyyy", Locale.US).format(end.getTime());
    }

    private boolean isSelectedMonthPast(Calendar monthCalendar) {
        Calendar now = Calendar.getInstance();
        int y1 = monthCalendar.get(Calendar.YEAR);
        int y2 = now.get(Calendar.YEAR);
        if (y1 != y2) {
            return y1 < y2;
        }
        return monthCalendar.get(Calendar.MONTH) < now.get(Calendar.MONTH);
    }

    private String safeText(String text, String fallback) {
        if (text == null || text.trim().isEmpty()) {
            return fallback;
        }
        return text.trim();
    }

    private String formatDays(double value) {
        if (Math.abs(value - Math.round(value)) < 0.001d) {
            return String.valueOf((long) Math.round(value));
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private String formatCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        formatter.setMaximumFractionDigits(2);
        formatter.setMinimumFractionDigits(0);
        return formatter.format(Math.max(0d, amount));
    }

    private String getWalletPaymentFailureReason(double requiredAmount, double availableBalance) {
        if (requiredAmount <= 0d) return "";
        if (availableBalance <= 0d) {
            return getString(R.string.payment_failed_wallet_zero);
        }
        if (availableBalance + 0.0001d < requiredAmount) {
            return getString(
                    R.string.payment_failed_wallet_insufficient,
                    formatCurrency(requiredAmount),
                    formatCurrency(availableBalance)
            );
        }
        return "";
    }

    private void showPaymentFailurePopup(String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.payment_failed)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private boolean onNavItemSelected(int id) {
        if (id == R.id.nav_dashboard) {
            navigateTo(DashboardActivity.class);
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
    }


    private void navigateTo(Class<? extends Activity> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void openNotificationScreen() {
        startActivity(new Intent(this, NotificationActivity.class));
    }

    private void updateNotificationDot() {
        if (notificationDot != null) {
            notificationDot.setVisibility(NotificationStore.hasUnread(this) ? View.VISIBLE : View.GONE);
        }
    }

}
