package com.example.majuri_app;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WorkerReportActivity extends AppCompatActivity {
    public static final String EXTRA_WORKER_ID = "extra_worker_id";
    public static final String EXTRA_SITE_NAME = "extra_site_name";
    public static final String EXTRA_FROM_DATE = "extra_from_date";
    public static final String EXTRA_TO_DATE = "extra_to_date";

    private final SimpleDateFormat inputDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private final SimpleDateFormat generatedDateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_report);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnShare).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.menu), Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnDownload).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.download_all_pdf_reports), Toast.LENGTH_SHORT).show());

        bindWorkerReport();
    }

    private void bindWorkerReport() {
        long workerId = getIntent() != null ? getIntent().getLongExtra(EXTRA_WORKER_ID, -1L) : -1L;
        if (workerId <= 0L) {
            Toast.makeText(this, R.string.worker_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String siteName = safeText(getIntent() != null ? getIntent().getStringExtra(EXTRA_SITE_NAME) : "", "Main Site");
        String fromDate = safeText(getIntent() != null ? getIntent().getStringExtra(EXTRA_FROM_DATE) : "", "");
        String toDate = safeText(getIntent() != null ? getIntent().getStringExtra(EXTRA_TO_DATE) : "", "");

        Calendar reportMonth = resolveReportMonth(fromDate, toDate);
        int year = reportMonth.get(Calendar.YEAR);
        int monthZeroBased = reportMonth.get(Calendar.MONTH);

        WorkerDbHelper dbHelper = new WorkerDbHelper(this);
        WorkerProfile profile = dbHelper.getWorkerProfileById(workerId);
        List<WorkerPaymentSummary> summaries = dbHelper.getWorkerPaymentSummariesForMonth(year, monthZeroBased);
        WorkerPaymentSummary summary = findSummaryByWorkerId(summaries, workerId);
        int[] attendanceCounts = dbHelper.getWorkerAttendanceCountsForMonth(workerId, year, monthZeroBased);
        dbHelper.close();

        String workerName = profile != null ? safeText(profile.getFullName(), "Worker") :
                (summary != null ? safeText(summary.getWorkerName(), "Worker") : "Worker");
        String phone = profile != null ? safeText(profile.getPhone(), "-") : "-";
        String employeeId = "EMP-" + String.format(Locale.US, "%06d", workerId);

        int presentCount = attendanceCounts[0];
        int halfDayCount = attendanceCounts[1];
        int absentCount = attendanceCounts[2];
        int workingEntries = presentCount + halfDayCount + absentCount;
        double presentEquivalent = presentCount + (halfDayCount * 0.5d);

        double dailyWage = summary != null ? summary.getDailyWage() : 0d;
        double gross = summary != null ? summary.getGrossAmount() : 0d;
        double paid = summary != null ? summary.getPaidAmount() : 0d;
        double pending = summary != null ? summary.getPendingAmount() : 0d;

        setText(R.id.tvReportWorkerName, workerName);
        setText(R.id.tvReportEmployeeId, employeeId);
        setText(R.id.tvReportPhone, phone);
        setText(R.id.tvReportSite, siteName);

        setText(R.id.tvWorkingDays, formatNumber(workingEntries));
        setText(R.id.tvPresentDays, formatNumber(presentEquivalent));
        setText(R.id.tvAbsentDays, String.format(Locale.US, "%02d", Math.max(absentCount, 0)));
        setText(R.id.tvOvertimeHours, "0h");

        setText(R.id.tvDailyWage, formatCurrency(dailyWage));
        setText(R.id.tvGrossSalary, formatCurrency(gross));
        setText(R.id.tvPaidAmount, formatCurrency(paid));
        setText(R.id.tvPendingAmount, formatCurrency(pending));
        setText(R.id.tvGeneratedDate, generatedDateFormat.format(new Date()));
    }

    private WorkerPaymentSummary findSummaryByWorkerId(List<WorkerPaymentSummary> summaries, long workerId) {
        if (summaries == null) return null;
        for (WorkerPaymentSummary item : summaries) {
            if (item != null && item.getWorkerId() == workerId) {
                return item;
            }
        }
        return null;
    }

    private Calendar resolveReportMonth(String fromDate, String toDate) {
        Calendar calendar = Calendar.getInstance();
        Date parsed = parseDate(toDate);
        if (parsed == null) {
            parsed = parseDate(fromDate);
        }
        if (parsed != null) {
            calendar.setTime(parsed);
        }
        return calendar;
    }

    private Date parseDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            return inputDateFormat.parse(raw.trim());
        } catch (ParseException ignored) {
            return null;
        }
    }

    private String formatCurrency(double value) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
        return formatter.format(Math.max(0d, value));
    }

    private String formatNumber(double value) {
        if (Math.abs(value - Math.round(value)) < 0.001d) {
            return String.valueOf((long) Math.round(value));
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        return value.trim();
    }

    private void setText(int viewId, String value) {
        View view = findViewById(viewId);
        if (view instanceof TextView) {
            ((TextView) view).setText(value);
        }
    }
}
