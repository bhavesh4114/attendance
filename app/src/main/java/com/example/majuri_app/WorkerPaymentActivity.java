package com.example.majuri_app;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class WorkerPaymentActivity extends AppCompatActivity implements PaymentResultListener {

    public static final String EXTRA_WORKER_ID = "extra_worker_id";
    public static final String EXTRA_WORKER_NAME = "extra_worker_name";
    public static final String EXTRA_WORKER_ROLE = "extra_worker_role";
    public static final String EXTRA_WORKER_CODE = "extra_worker_code";
    public static final String EXTRA_DAILY_WAGE = "extra_daily_wage";
    public static final String EXTRA_ATTENDANCE_STATUS = "extra_attendance_status";

    private TextView workerName;
    private TextView workerIdRole;
    private TextView tvDailyWage;
    private TextView tvTotalAttendance;
    private TextView tvGrossWage;
    private TextView tvAdvanceDeductions;
    private TextView tvNetPayable;
    private View optionCash;
    private View optionUpi;
    private MaterialButton btnMarkAsPaid;
    private boolean isCashSelected = true;
    private long currentWorkerId = -1L;
    private String currentWorkerName = "Worker";
    private double currentPendingAmount = 0d;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_payment);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        bindViews();
        bindWorkerData();
        bindPaymentMethodSelection();
        bindMarkAsPaid();
        Checkout.preload(getApplicationContext());
    }

    private void bindViews() {
        workerName = findViewById(R.id.workerName);
        workerIdRole = findViewById(R.id.workerIdRole);
        tvDailyWage = findViewById(R.id.tvDailyWage);
        tvTotalAttendance = findViewById(R.id.tvTotalAttendance);
        tvGrossWage = findViewById(R.id.tvGrossWage);
        tvAdvanceDeductions = findViewById(R.id.tvAdvanceDeductions);
        tvNetPayable = findViewById(R.id.tvNetPayable);
        optionCash = findViewById(R.id.optionCash);
        optionUpi = findViewById(R.id.optionUpi);
        btnMarkAsPaid = findViewById(R.id.btnMarkAsPaid);
    }

    private void bindPaymentMethodSelection() {
        if (optionCash != null) {
            optionCash.setOnClickListener(v -> setPaymentMethodSelection(true));
        }
        if (optionUpi != null) {
            optionUpi.setOnClickListener(v -> {
                setPaymentMethodSelection(false);
                if (currentPendingAmount > 0d) {
                    startRazorpayCheckout();
                }
            });
        }
        setPaymentMethodSelection(true);
    }

    private void setPaymentMethodSelection(boolean selectCash) {
        isCashSelected = selectCash;
        if (optionCash != null) {
            optionCash.setBackgroundResource(
                    isCashSelected ? R.drawable.bg_payment_option_selected : R.drawable.bg_payment_option_unselected
            );
        }
        if (optionUpi != null) {
            optionUpi.setBackgroundResource(
                    isCashSelected ? R.drawable.bg_payment_option_unselected : R.drawable.bg_payment_option_selected
            );
        }
        if (btnMarkAsPaid != null) {
            btnMarkAsPaid.setText(isCashSelected ? getString(R.string.mark_as_paid) : "Pay Now");
        }
    }

    private void bindMarkAsPaid() {
        if (btnMarkAsPaid != null) {
            btnMarkAsPaid.setOnClickListener(v -> handleMarkAsPaid());
        }
    }

    private void handleMarkAsPaid() {
        if (currentWorkerId <= 0L || currentPendingAmount <= 0d) {
            Toast.makeText(this, R.string.no_pending_payments, Toast.LENGTH_SHORT).show();
            return;
        }
        if (isCashSelected) {
            recordPayment(currentPendingAmount, "Cash");
            return;
        }
        startRazorpayCheckout();
    }

    private void startRazorpayCheckout() {
        String keyId = getString(R.string.razorpay_key_id).trim();
        if (keyId.isEmpty() || keyId.contains("your_key_here")) {
            Toast.makeText(this, R.string.configure_razorpay_key, Toast.LENGTH_LONG).show();
            return;
        }
        try {
            Checkout checkout = new Checkout();
            checkout.setKeyID(keyId);
            JSONObject options = new JSONObject();
            options.put("name", getString(R.string.app_name));
            options.put("description", currentWorkerName + " wage payment");
            options.put("currency", "INR");
            options.put("amount", (long) Math.round(currentPendingAmount * 100d));
            checkout.open(this, options);
        } catch (Exception e) {
            Toast.makeText(this, R.string.payment_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void bindWorkerData() {
        long workerId = getIntent().getLongExtra(EXTRA_WORKER_ID, -1L);
        String passedName = getIntent().getStringExtra(EXTRA_WORKER_NAME);
        String passedRole = getIntent().getStringExtra(EXTRA_WORKER_ROLE);
        String passedCode = getIntent().getStringExtra(EXTRA_WORKER_CODE);
        double passedDailyWage = getIntent().getDoubleExtra(EXTRA_DAILY_WAGE, -1d);
        int passedStatus = getIntent().getIntExtra(EXTRA_ATTENDANCE_STATUS, AttendanceStaffItem.STATUS_PRESENT);

        Calendar now = Calendar.getInstance();
        WorkerDbHelper dbHelper = new WorkerDbHelper(this);
        List<WorkerPaymentSummary> summaries = dbHelper.getWorkerPaymentSummariesForMonth(
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH)
        );
        double workerDailyWage = dbHelper.getWorkerDailyWageById(workerId);
        dbHelper.close();

        WorkerPaymentSummary target = null;
        for (WorkerPaymentSummary summary : summaries) {
            if (summary != null && summary.getWorkerId() == workerId) {
                target = summary;
                break;
            }
        }

        String displayName = safeText(passedName, "Worker");
        String displayRole = safeText(passedRole, "Worker");
        String displayCode = safeText(passedCode, workerId > 0L ? "#WR-" + workerId : "#WR-0");

        double dailyWage = 0d;
        double workedDays = 0d;
        double grossAmount = 0d;
        double paidAmount = 0d;
        double pendingAmount = 0d;

        if (target != null) {
            displayName = safeText(target.getWorkerName(), displayName);
            displayRole = safeText(target.getRole(), displayRole);
            workedDays = target.getWorkedDays();
            grossAmount = target.getGrossAmount();
            paidAmount = target.getPaidAmount();
            pendingAmount = target.getPendingAmount();
        }
        if (passedDailyWage >= 0d) {
            dailyWage = passedDailyWage;
        } else {
            dailyWage = workerDailyWage > 0d ? workerDailyWage : dailyWage;
        }
        if (workedDays <= 0d && dailyWage > 0d) {
            workedDays = workedDaysFromStatus(passedStatus);
            grossAmount = dailyWage * workedDays;
            pendingAmount = Math.max(0d, grossAmount - paidAmount);
        }

        currentWorkerId = workerId;
        currentWorkerName = displayName;
        currentPendingAmount = Math.max(0d, pendingAmount);

        workerName.setText(displayName);
        workerIdRole.setText("ID: " + displayCode + " \u2022 " + displayRole);
        tvDailyWage.setText(formatCurrency(dailyWage) + " / Day");
        tvTotalAttendance.setText(formatDays(workedDays) + " Days");
        tvGrossWage.setText(formatCurrency(grossAmount));
        tvAdvanceDeductions.setText("-" + formatCurrency(paidAmount));
        tvNetPayable.setText(formatPlainAmount(pendingAmount));
    }

    @Override
    public void onPaymentSuccess(String razorpayPaymentId) {
        String note = "UPI via Razorpay";
        if (razorpayPaymentId != null && !razorpayPaymentId.trim().isEmpty()) {
            note = note + " (" + razorpayPaymentId.trim() + ")";
        }
        recordPayment(currentPendingAmount, note);
    }

    @Override
    public void onPaymentError(int code, String response) {
        Toast.makeText(this, R.string.payment_failed, Toast.LENGTH_SHORT).show();
    }

    private void recordPayment(double amount, String note) {
        WorkerDbHelper dbHelper = new WorkerDbHelper(this);
        long rowId = dbHelper.recordAdvancePayment(
                currentWorkerId,
                amount,
                getTodayIsoDate(),
                note
        );
        dbHelper.close();

        if (rowId > 0L) {
            Toast.makeText(this, R.string.payment_successful, Toast.LENGTH_SHORT).show();
            bindWorkerData();
            return;
        }
        Toast.makeText(this, R.string.payment_record_failed, Toast.LENGTH_SHORT).show();
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
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

    private double workedDaysFromStatus(int status) {
        if (status == AttendanceStaffItem.STATUS_HALF_DAY) {
            return 0.5d;
        }
        if (status == AttendanceStaffItem.STATUS_ABSENT) {
            return 0d;
        }
        return 1d;
    }

    private String formatPlainAmount(double amount) {
        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("en", "IN"));
        formatter.setMaximumFractionDigits(2);
        formatter.setMinimumFractionDigits(0);
        return formatter.format(Math.max(0d, amount));
    }

    private String getTodayIsoDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());
    }
}
