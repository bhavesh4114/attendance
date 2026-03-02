package com.example.majuri_app;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
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
            return String.valueOf(Math.round(value));
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

        String workerName = getViewText(R.id.tvReportWorkerName);
        String employeeId = getViewText(R.id.tvReportEmployeeId);
        String phone = getViewText(R.id.tvReportPhone);
        String site = getViewText(R.id.tvReportSite);
        String present = getViewText(R.id.tvPresentDays);
        String absent = getViewText(R.id.tvAbsentDays);
        String overtime = getViewText(R.id.tvOvertimeHours);
        String dailyWage = getViewText(R.id.tvDailyWage);
        String grossSalary = getViewText(R.id.tvGrossSalary);
        String paidAmount = getViewText(R.id.tvPaidAmount);
        String pendingAmount = getViewText(R.id.tvPendingAmount);
        String generatedOn = getViewText(R.id.tvGeneratedDate);

        Paint headerFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerFillPaint.setColor(Color.parseColor("#1E40AF"));
        headerFillPaint.setStyle(Paint.Style.FILL);

        Paint cardFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardFillPaint.setColor(Color.parseColor("#F3F4F6"));
        cardFillPaint.setStyle(Paint.Style.FILL);

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.parseColor("#D1D5DB"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.2f);

        Paint companyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        companyPaint.setColor(Color.WHITE);
        companyPaint.setTextSize(16f);
        companyPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint invoiceTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        invoiceTitlePaint.setColor(Color.WHITE);
        invoiceTitlePaint.setTextSize(30f);
        invoiceTitlePaint.setTextAlign(Paint.Align.RIGHT);
        invoiceTitlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint sectionTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sectionTitlePaint.setColor(Color.parseColor("#111827"));
        sectionTitlePaint.setTextSize(13f);
        sectionTitlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.parseColor("#4B5563"));
        labelPaint.setTextSize(11f);

        Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(Color.parseColor("#111827"));
        valuePaint.setTextSize(11f);
        valuePaint.setTextAlign(Paint.Align.RIGHT);
        valuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint totalLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        totalLabelPaint.setColor(Color.parseColor("#1F2937"));
        totalLabelPaint.setTextSize(13f);
        totalLabelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint totalValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        totalValuePaint.setColor(Color.parseColor("#0B1329"));
        totalValuePaint.setTextSize(20f);
        totalValuePaint.setTextAlign(Paint.Align.RIGHT);
        totalValuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint headerMetaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerMetaPaint.setColor(Color.parseColor("#DBEAFE"));
        headerMetaPaint.setTextSize(10f);

        Paint mutedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mutedPaint.setColor(Color.parseColor("#6B7280"));
        mutedPaint.setTextSize(10f);

        int pageWidth = pageInfo.getPageWidth();
        int pageHeight = pageInfo.getPageHeight();
        int margin = 36;
        int contentLeft = margin;
        int contentRight = pageWidth - margin;
        int contentWidth = contentRight - contentLeft;

        RectF headerRect = new RectF(contentLeft, margin, contentRight, margin + 88);
        canvas.drawRoundRect(headerRect, 12f, 12f, headerFillPaint);
        canvas.drawText(resolveCompanyName(), contentLeft + 18, margin + 35, companyPaint);
        canvas.drawText("Professional Labour Services", contentLeft + 18, margin + 56, headerMetaPaint);
        canvas.drawText("INVOICE", contentRight - 18, margin + 54, invoiceTitlePaint);
        canvas.drawText("Generated: " + safeText(generatedOn, "-"), contentRight - 18, margin + 74, headerMetaPaint);

        int y = (int) headerRect.bottom + 22;
        int cardPadding = 12;
        int rowHeight = 28;

        String[] workerLabels = {"Worker Name", "Employee ID", "Phone Number", "Current Site"};
        String[] workerValues = {
                safeText(workerName, "-"),
                safeText(employeeId, "-"),
                safeText(phone, "-"),
                safeText(site, "-")
        };
        y = drawTableCard(
                canvas, contentLeft, contentRight, y,
                "Worker Information", workerLabels, workerValues,
                cardFillPaint, borderPaint, sectionTitlePaint, labelPaint, valuePaint,
                cardPadding, rowHeight
        );

        y += 16;
        String[] paymentLabels = {"Daily Wage Rate", "Gross Salary", "Total Paid Amount", "Present Days", "Absent Days", "Overtime"};
        String[] paymentValues = {
                safeText(dailyWage, "-"),
                safeText(grossSalary, "-"),
                safeText(paidAmount, "-"),
                safeText(present, "-"),
                safeText(absent, "-"),
                safeText(overtime, "-")
        };
        y = drawTableCard(
                canvas, contentLeft, contentRight, y,
                "Payment Details", paymentLabels, paymentValues,
                cardFillPaint, borderPaint, sectionTitlePaint, labelPaint, valuePaint,
                cardPadding, rowHeight
        );

        y += 14;
        RectF totalRect = new RectF(contentLeft, y, contentRight, y + 50);
        Paint totalFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        totalFillPaint.setColor(Color.parseColor("#E5EDFF"));
        totalFillPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(totalRect, 8f, 8f, totalFillPaint);
        canvas.drawRoundRect(totalRect, 8f, 8f, borderPaint);
        canvas.drawText("TOTAL AMOUNT DUE", totalRect.left + 14, totalRect.top + 31, totalLabelPaint);
        canvas.drawText(safeText(pendingAmount, "$0.00"), totalRect.right - 14, totalRect.top + 34, totalValuePaint);

        int signatureY = Math.min(pageHeight - 92, (int) totalRect.bottom + 72);
        canvas.drawText("Date: " + safeText(generatedOn, "-"), contentLeft, signatureY, labelPaint);
        canvas.drawLine(contentRight - 180, signatureY - 10, contentRight, signatureY - 10, borderPaint);
        canvas.drawText("Authorized Signature", contentRight - 90, signatureY + 12, mutedPaint);

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

    private int drawTableCard(
            Canvas canvas,
            int left,
            int right,
            int top,
            String title,
            String[] labels,
            String[] values,
            Paint cardFillPaint,
            Paint borderPaint,
            Paint sectionTitlePaint,
            Paint labelPaint,
            Paint valuePaint,
            int cardPadding,
            int rowHeight
    ) {
        int rows = Math.min(labels != null ? labels.length : 0, values != null ? values.length : 0);
        int headerHeight = 30;
        int tableHeight = rows * rowHeight;
        int cardHeight = headerHeight + tableHeight + (cardPadding * 2);

        RectF cardRect = new RectF(left, top, right, top + cardHeight);
        canvas.drawRoundRect(cardRect, 10f, 10f, cardFillPaint);
        canvas.drawRoundRect(cardRect, 10f, 10f, borderPaint);

        float titleX = left + cardPadding;
        float titleY = top + cardPadding + 13;
        canvas.drawText(title, titleX, titleY, sectionTitlePaint);

        int tableLeft = left + cardPadding;
        int tableRight = right - cardPadding;
        int tableTop = top + cardPadding + headerHeight - 2;
        int tableBottom = tableTop + tableHeight;
        int splitX = tableLeft + (int) ((tableRight - tableLeft) * 0.58f);

        canvas.drawRect(tableLeft, tableTop, tableRight, tableBottom, borderPaint);
        canvas.drawLine(splitX, tableTop, splitX, tableBottom, borderPaint);

        for (int i = 1; i < rows; i++) {
            int rowY = tableTop + (i * rowHeight);
            canvas.drawLine(tableLeft, rowY, tableRight, rowY, borderPaint);
        }

        for (int i = 0; i < rows; i++) {
            float baseline = tableTop + (i * rowHeight) + 18;
            canvas.drawText(safeText(labels[i], "-"), tableLeft + 10, baseline, labelPaint);
            canvas.drawText(safeText(values[i], "-"), tableRight - 10, baseline, valuePaint);
        }

        return (int) cardRect.bottom;
    }

    private String resolveCompanyName() {
        BuilderDbHelper helper = new BuilderDbHelper(this);
        try {
            List<String> names = helper.getAllBusinessNames();
            if (names != null && !names.isEmpty()) {
                String name = names.get(0);
                if (name != null && !name.trim().isEmpty()) {
                    return name.trim();
                }
            }
        } catch (Exception ignored) {
            // Fall back to app name.
        } finally {
            helper.close();
        }
        return getString(R.string.app_name);
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
        Uri collection = android.provider.MediaStore.Files.getContentUri("external");
        Uri itemUri = resolver.insert(collection, values);
        if (itemUri == null) {
            return false;
        }

        try (OutputStream outputStream = resolver.openOutputStream(itemUri)) {
            if (outputStream == null) return false;
            document.writeTo(outputStream);
            values.clear();
            values.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(itemUri, values, null, null);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean savePdfLegacy(PdfDocument document, String fileName) {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File targetDir = new File(downloadsDir, "AttendanceReports");
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            return false;
        }

        File outputFile = new File(targetDir, fileName);
        try (OutputStream outputStream = new FileOutputStream(outputFile)) {
            document.writeTo(outputStream);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
