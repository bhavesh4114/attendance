package com.example.majuri_app;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
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
    private ActivityResultLauncher<String> storagePermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_report);

        storagePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        generateAndSavePdfReport();
                    } else {
                        Toast.makeText(this, R.string.storage_permission_required, Toast.LENGTH_SHORT).show();
                    }
                }
        );

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnShare).setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.menu), Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnDownload).setOnClickListener(v -> onDownloadPdfClicked());

        bindWorkerReport();
    }

    private void onDownloadPdfClicked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            generateAndSavePdfReport();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            generateAndSavePdfReport();
        } else {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
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

    private void generateAndSavePdfReport() {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.parseColor("#0D1324"));
        titlePaint.setTextSize(20f);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint sectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sectionPaint.setColor(Color.parseColor("#1F2937"));
        sectionPaint.setTextSize(14f);
        sectionPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(Color.parseColor("#111827"));
        bodyPaint.setTextSize(12f);

        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#9CA3AF"));
        linePaint.setStrokeWidth(1.5f);

        int left = 40;
        int right = 555;
        int y = 50;

        canvas.drawText("Worker Attendance Report", left, y, titlePaint);
        y += 24;
        canvas.drawLine(left, y, right, y, linePaint);
        y += 24;

        String workerName = getViewText(R.id.tvReportWorkerName);
        String present = getViewText(R.id.tvPresentDays);
        String absent = getViewText(R.id.tvAbsentDays);
        String overtime = getViewText(R.id.tvOvertimeHours);
        String dailyWage = getViewText(R.id.tvDailyWage);
        String grossSalary = getViewText(R.id.tvGrossSalary);
        String paidAmount = getViewText(R.id.tvPaidAmount);
        String pendingAmount = getViewText(R.id.tvPendingAmount);
        String generatedOn = getViewText(R.id.tvGeneratedDate);

        y = drawSectionTitle(canvas, "Worker Details", left, y, sectionPaint);
        y = drawKeyValue(canvas, "Worker Name", workerName, left, y, bodyPaint);

        y += 8;
        y = drawSectionTitle(canvas, "Attendance Summary", left, y, sectionPaint);
        y = drawKeyValue(canvas, "Present", present, left, y, bodyPaint);
        y = drawKeyValue(canvas, "Absent", absent, left, y, bodyPaint);
        y = drawKeyValue(canvas, "Overtime", overtime, left, y, bodyPaint);

        y += 8;
        y = drawSectionTitle(canvas, "Payment Details", left, y, sectionPaint);
        y = drawKeyValue(canvas, "Daily Wage", dailyWage, left, y, bodyPaint);
        y = drawKeyValue(canvas, "Gross Salary", grossSalary, left, y, bodyPaint);
        y = drawKeyValue(canvas, "Paid Amount", paidAmount, left, y, bodyPaint);
        y = drawKeyValue(canvas, "Pending Amount", pendingAmount, left, y, bodyPaint);

        y += 8;
        y = drawSectionTitle(canvas, "Generated", left, y, sectionPaint);
        y = drawKeyValue(canvas, "Date Generated", generatedOn, left, y, bodyPaint);

        int signatureY = y + 60;
        canvas.drawLine(right - 180, signatureY, right, signatureY, linePaint);
        canvas.drawText("Authorized Signature", right - 165, signatureY + 20, bodyPaint);

        document.finishPage(page);

        String fileName = "Worker_Report_" + System.currentTimeMillis() + ".pdf";
        boolean success = savePdfToDownloads(document, fileName);
        document.close();

        if (success) {
            Toast.makeText(this, getString(R.string.pdf_saved_success, fileName), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.failed_to_save_pdf, Toast.LENGTH_SHORT).show();
        }
    }

    private int drawSectionTitle(Canvas canvas, String title, int left, int y, Paint paint) {
        canvas.drawText(title, left, y, paint);
        return y + 20;
    }

    private int drawKeyValue(Canvas canvas, String key, String value, int left, int y, Paint paint) {
        String safeValue = (value == null || value.trim().isEmpty()) ? "-" : value.trim();
        canvas.drawText(key + ": " + safeValue, left, y, paint);
        return y + 18;
    }

    @NonNull
    private String getViewText(int viewId) {
        View view = findViewById(viewId);
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            return text != null ? text.toString() : "";
        }
        return "";
    }

    private boolean savePdfToDownloads(PdfDocument document, String fileName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return savePdfWithMediaStore(document, fileName);
        }
        return savePdfLegacy(document, fileName);
    }

    private boolean savePdfWithMediaStore(PdfDocument document, String fileName) {
        ContentValues values = new ContentValues();
        values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AttendanceReports");
        values.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1);

        ContentResolver resolver = getContentResolver();
        Uri collection = android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        Uri itemUri = resolver.insert(collection, values);
        if (itemUri == null) {
            return false;
        }

        OutputStream outputStream = null;
        try {
            outputStream = resolver.openOutputStream(itemUri);
            if (outputStream == null) return false;
            document.writeTo(outputStream);
            values.clear();
            values.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(itemUri, values, null, null);
            return true;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception ignored) {
                    // Ignore close exception
                }
            }
        }
    }

    private boolean savePdfLegacy(PdfDocument document, String fileName) {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File targetDir = new File(downloadsDir, "AttendanceReports");
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            return false;
        }

        File outputFile = new File(targetDir, fileName);
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(outputFile);
            document.writeTo(outputStream);
            return true;
        } catch (Exception ignored) {
            return false;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception ignored) {
                    // Ignore close exception
                }
            }
        }
    }
}
