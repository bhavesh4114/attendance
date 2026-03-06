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
        double overtimeHours = summary != null ? summary.getOvertimeHours() : 0d;
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
        setText(R.id.tvOvertimeHours, formatNumber(overtimeHours) + "h");

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
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
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
        String dailyWageText = getViewText(R.id.tvDailyWage);
        String grossSalaryText = getViewText(R.id.tvGrossSalary);
        String paidAmountText = getViewText(R.id.tvPaidAmount);
        String pendingAmountText = getViewText(R.id.tvPendingAmount);
        String generatedOn = getViewText(R.id.tvGeneratedDate);

        double dailyWage = parseAmount(dailyWageText);
        double grossSalary = parseAmount(grossSalaryText);
        double paidAmount = parseAmount(paidAmountText);
        double pendingAmount = parseAmount(pendingAmountText);
        double presentDays = parseAmount(present);
        double absentDays = parseAmount(absent);
        double overtimeHours = parseAmount(overtime);
        double hourlyRate = dailyWage > 0d ? (dailyWage / 8d) : 0d;
        if (hourlyRate <= 0d) hourlyRate = 250d;

        double[] rowHours = {8.0, 8.0, 4.0, 2.0, 6.5};
        String[] rowTypes = {"Full Day", "Full Day", "Half Day", "Overtime", "Hourly"};
        double[] rowRate = {
                hourlyRate,
                hourlyRate,
                hourlyRate,
                hourlyRate * 1.5d,
                hourlyRate
        };
        double[] rowAmount = new double[rowHours.length];
        for (int i = 0; i < rowHours.length; i++) {
            rowAmount[i] = rowHours[i] * rowRate[i];
        }
        if (grossSalary <= 0d) {
            double sum = 0d;
            for (double amount : rowAmount) sum += amount;
            grossSalary = sum;
        }
        if (pendingAmount <= 0d) {
            pendingAmount = Math.max(0d, grossSalary - paidAmount);
        }

        Paint pageBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pageBgPaint.setColor(Color.parseColor("#EEF2F7"));
        canvas.drawRect(0, 0, pageInfo.getPageWidth(), pageInfo.getPageHeight(), pageBgPaint);

        Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardPaint.setColor(Color.WHITE);
        cardPaint.setStyle(Paint.Style.FILL);
        Paint cardBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardBorderPaint.setColor(Color.parseColor("#D4DBE5"));
        cardBorderPaint.setStyle(Paint.Style.STROKE);
        cardBorderPaint.setStrokeWidth(1f);

        Paint darkHeaderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        darkHeaderPaint.setColor(Color.parseColor("#0B1736"));
        darkHeaderPaint.setStyle(Paint.Style.FILL);

        Paint whiteBoldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whiteBoldPaint.setColor(Color.WHITE);
        whiteBoldPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint bodyDarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyDarkPaint.setColor(Color.parseColor("#111827"));
        bodyDarkPaint.setTextSize(10.5f);

        Paint bodyMutedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyMutedPaint.setColor(Color.parseColor("#64748B"));
        bodyMutedPaint.setTextSize(10f);

        Paint blueBoldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blueBoldPaint.setColor(Color.parseColor("#1D4ED8"));
        blueBoldPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.parseColor("#D4DBE5"));
        linePaint.setStrokeWidth(1f);

        int pageLeft = 18;
        int pageRight = pageInfo.getPageWidth() - 18;
        int pageTop = 16;
        int pageBottom = pageInfo.getPageHeight() - 16;

        RectF mainCard = new RectF(pageLeft, pageTop, pageRight, pageBottom);
        canvas.drawRoundRect(mainCard, 12f, 12f, cardPaint);
        canvas.drawRoundRect(mainCard, 12f, 12f, cardBorderPaint);

        RectF headerRect = new RectF(mainCard.left, mainCard.top, mainCard.right, mainCard.top + 118);
        canvas.drawRoundRect(headerRect, 12f, 12f, darkHeaderPaint);
        canvas.drawRect(headerRect.left, headerRect.bottom - 12, headerRect.right, headerRect.bottom, darkHeaderPaint);

        whiteBoldPaint.setTextSize(18f);
        canvas.drawText(resolveCompanyName().toUpperCase(Locale.US), headerRect.left + 18, headerRect.top + 30, whiteBoldPaint);
        whiteBoldPaint.setTextSize(11f);
        canvas.drawText("Industrial Parkway, Building B", headerRect.left + 18, headerRect.top + 50, whiteBoldPaint);
        canvas.drawText("New York, NY 10001", headerRect.left + 18, headerRect.top + 66, whiteBoldPaint);
        canvas.drawText("+1 (555) 0123-4567", headerRect.left + 18, headerRect.top + 82, whiteBoldPaint);

        blueBoldPaint.setTextSize(19f);
        blueBoldPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("ATTENDANCE", headerRect.right - 16, headerRect.top + 44, blueBoldPaint);
        canvas.drawText("REPORT", headerRect.right - 16, headerRect.top + 70, blueBoldPaint);
        bodyMutedPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Date Issued: " + safeText(generatedOn, "-"), headerRect.right - 16, headerRect.top + 90, bodyMutedPaint);
        canvas.drawText("Report ID: " + buildReportId(), headerRect.right - 16, headerRect.top + 105, bodyMutedPaint);
        bodyMutedPaint.setTextAlign(Paint.Align.LEFT);
        blueBoldPaint.setTextAlign(Paint.Align.LEFT);

        float contentLeft = mainCard.left + 16;
        float contentRight = mainCard.right - 16;
        float y = headerRect.bottom + 18;

        RectF infoRect = new RectF(contentLeft, y, contentRight, y + 132);
        Paint infoFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        infoFillPaint.setColor(Color.parseColor("#F6F8FB"));
        canvas.drawRoundRect(infoRect, 10f, 10f, infoFillPaint);
        canvas.drawRoundRect(infoRect, 10f, 10f, cardBorderPaint);

        blueBoldPaint.setTextSize(12f);
        canvas.drawText("WORKER INFORMATION", infoRect.left + 14, infoRect.top + 24, blueBoldPaint);
        canvas.drawLine(infoRect.left + 14, infoRect.top + 34, infoRect.right - 14, infoRect.top + 34, linePaint);

        float colGap = 16f;
        float colWidth = (infoRect.width() - 28f - colGap) / 2f;
        float lColX = infoRect.left + 14;
        float rColX = lColX + colWidth + colGap;
        float row1Y = infoRect.top + 56;
        float row2Y = infoRect.top + 96;

        drawLabelValueBlock(canvas, lColX, row1Y, "Worker\nName", safeText(workerName, "-"), bodyMutedPaint, bodyDarkPaint);
        drawLabelValueBlock(canvas, rColX, row1Y, "Worker\nID", safeText(employeeId, "-"), bodyMutedPaint, bodyDarkPaint);
        drawLabelValueBlock(canvas, lColX, row2Y, "Role", "Senior\nMason", bodyMutedPaint, bodyDarkPaint);
        drawLabelValueBlock(canvas, rColX, row2Y, "Site\nLocation", safeText(site, "-"), bodyMutedPaint, bodyDarkPaint);
        canvas.drawLine(infoRect.left + 14, infoRect.top + 74, infoRect.right - 14, infoRect.top + 74, linePaint);

        y = infoRect.bottom + 18;
        float metricGap = 10f;
        float metricWidth = (contentRight - contentLeft - (metricGap * 3f)) / 4f;
        String[] metricTitles = {"FULL\nDAYS", "HALF\nDAYS", "HOURLY", "OVERTIME"};
        String[] metricValues = {
                String.valueOf((int) Math.max(0, Math.round(presentDays + absentDays))),
                "02",
                formatNumber(presentDays) + "h",
                formatNumber(overtimeHours) + "h"
        };
        for (int i = 0; i < 4; i++) {
            float left = contentLeft + i * (metricWidth + metricGap);
            RectF metricRect = new RectF(left, y, left + metricWidth, y + 60);
            canvas.drawRoundRect(metricRect, 8f, 8f, infoFillPaint);
            canvas.drawRoundRect(metricRect, 8f, 8f, cardBorderPaint);
            bodyMutedPaint.setTextAlign(Paint.Align.CENTER);
            bodyMutedPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(metricTitles[i], metricRect.centerX(), metricRect.top + 18, bodyMutedPaint);
            blueBoldPaint.setTextAlign(Paint.Align.CENTER);
            blueBoldPaint.setTextSize(14f);
            canvas.drawText(metricValues[i], metricRect.centerX(), metricRect.bottom - 12, blueBoldPaint);
        }
        bodyMutedPaint.setTextAlign(Paint.Align.LEFT);
        blueBoldPaint.setTextAlign(Paint.Align.LEFT);

        y += 78;
        RectF tableRect = new RectF(contentLeft, y, contentRight, y + 250);
        canvas.drawRoundRect(tableRect, 0f, 0f, cardPaint);
        canvas.drawRect(tableRect, cardBorderPaint);

        Paint tableHeaderFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        tableHeaderFill.setColor(Color.parseColor("#EAF0F7"));
        RectF tableHeaderRect = new RectF(tableRect.left, tableRect.top, tableRect.right, tableRect.top + 30);
        canvas.drawRect(tableHeaderRect, tableHeaderFill);

        float[] colXs = {
                tableRect.left,
                tableRect.left + tableRect.width() * 0.2f,
                tableRect.left + tableRect.width() * 0.55f,
                tableRect.left + tableRect.width() * 0.73f,
                tableRect.left + tableRect.width() * 0.86f,
                tableRect.right
        };
        String[] headers = {"DATE", "ATTENDANCE\nTYPE", "HOURS", "RATE\n(₹)", "AMOUNT\n(₹)"};
        Paint headerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerTextPaint.setColor(Color.parseColor("#334155"));
        headerTextPaint.setTextSize(9f);
        headerTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        for (int i = 0; i < headers.length; i++) {
            canvas.drawText(headers[i], colXs[i] + 6, tableRect.top + 12, headerTextPaint);
        }
        canvas.drawLine(tableRect.left, tableRect.top + 30, tableRect.right, tableRect.top + 30, linePaint);
        for (int i = 1; i < colXs.length - 1; i++) {
            canvas.drawLine(colXs[i], tableRect.top, colXs[i], tableRect.bottom, linePaint);
        }

        Calendar rowCal = Calendar.getInstance();
        rowCal.set(Calendar.DAY_OF_MONTH, 1);
        SimpleDateFormat rowDateFormat = new SimpleDateFormat("MMM dd,\nyyyy", Locale.US);
        Paint rowTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rowTextPaint.setColor(Color.parseColor("#111827"));
        rowTextPaint.setTextSize(10f);

        float rowTop = tableRect.top + 30;
        float rowH = 44f;
        for (int i = 0; i < rowHours.length; i++) {
            float currentTop = rowTop + (i * rowH);
            float currentBottom = currentTop + rowH;
            if (i > 0) canvas.drawLine(tableRect.left, currentTop, tableRect.right, currentTop, linePaint);

            String dateLabel = rowDateFormat.format(rowCal.getTime());
            String[] dateLines = dateLabel.split("\n");
            canvas.drawText(dateLines[0], colXs[0] + 6, currentTop + 16, rowTextPaint);
            canvas.drawText(dateLines[1], colXs[0] + 6, currentTop + 30, rowTextPaint);

            canvas.drawText(rowTypes[i], colXs[1] + 6, currentTop + 24, rowTextPaint);
            canvas.drawText(formatNumber(rowHours[i]), colXs[2] + 6, currentTop + 24, rowTextPaint);
            canvas.drawText(formatNumber(rowRate[i]), colXs[3] + 6, currentTop + 24, rowTextPaint);
            canvas.drawText(formatNumber(rowAmount[i]), colXs[4] + 6, currentTop + 24, rowTextPaint);
            rowCal.add(Calendar.DAY_OF_MONTH, 1);
            if (currentBottom >= tableRect.bottom) break;
        }

        float summaryStartY = tableRect.bottom + 18;
        canvas.drawLine(contentLeft, summaryStartY, contentRight, summaryStartY, linePaint);
        summaryStartY += 22;

        drawSummaryLine(canvas, contentLeft, contentRight, summaryStartY, "Gross Earnings", formatCurrency(grossSalary), bodyMutedPaint, bodyDarkPaint, false);
        summaryStartY += 20;
        drawSummaryLine(canvas, contentLeft, contentRight, summaryStartY, "Total Deductions", "(" + formatCurrency(paidAmount) + ")", bodyMutedPaint, bodyDarkPaint, true);

        RectF netRect = new RectF(contentLeft, summaryStartY + 10, contentRight, summaryStartY + 44);
        Paint netFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        netFill.setColor(Color.parseColor("#E5EDFF"));
        canvas.drawRoundRect(netRect, 8f, 8f, netFill);
        canvas.drawRoundRect(netRect, 8f, 8f, cardBorderPaint);
        Paint netLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        netLabelPaint.setColor(Color.parseColor("#1E293B"));
        netLabelPaint.setTextSize(11f);
        netLabelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        Paint netValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        netValuePaint.setColor(Color.parseColor("#1D4ED8"));
        netValuePaint.setTextSize(18f);
        netValuePaint.setTextAlign(Paint.Align.RIGHT);
        netValuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Net Payable Amount", netRect.left + 10, netRect.top + 22, netLabelPaint);
        canvas.drawText(formatCurrency(pendingAmount), netRect.right - 10, netRect.top + 24, netValuePaint);

        float notesTop = netRect.bottom + 28;
        canvas.drawLine(contentLeft, notesTop, contentRight, notesTop, linePaint);
        notesTop += 26;
        Paint notesTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        notesTitlePaint.setColor(Color.parseColor("#64748B"));
        notesTitlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        notesTitlePaint.setTextSize(10f);
        canvas.drawText("NOTES & REMARKS", contentLeft, notesTop, notesTitlePaint);

        Paint notesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        notesPaint.setColor(Color.parseColor("#94A3B8"));
        notesPaint.setTextSize(9f);
        notesPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        String[] notesLines = {
                "The attendance recorded above is based",
                "on biometric entry and verified by",
                "site supervisor. Payments will be",
                "processed within 3-5 business days.",
                "Any discrepancy should be reported",
                "to HR within 24 hours."
        };
        float notesY = notesTop + 16;
        for (String line : notesLines) {
            canvas.drawText(line, contentLeft, notesY, notesPaint);
            notesY += 14;
        }

        RectF stampRect = new RectF(contentRight - 170, notesTop - 8, contentRight, notesTop + 78);
        Paint stampBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        stampBorder.setColor(Color.parseColor("#C9D4E2"));
        stampBorder.setStyle(Paint.Style.STROKE);
        stampBorder.setStrokeWidth(1f);
        canvas.drawRoundRect(stampRect, 4f, 4f, stampBorder);
        bodyMutedPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("COMPANY STAMP", stampRect.centerX(), stampRect.centerY(), bodyMutedPaint);
        bodyMutedPaint.setTextAlign(Paint.Align.LEFT);

        float sigY = notesTop + 106;
        canvas.drawLine(contentRight - 170, sigY, contentRight, sigY, linePaint);
        bodyDarkPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Authorized Signature", contentRight - 140, sigY + 16, bodyDarkPaint);
        bodyMutedPaint.setTextSize(9f);
        canvas.drawText("Site Operations Manager", contentRight - 140, sigY + 30, bodyMutedPaint);

        Paint footerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        footerPaint.setColor(Color.parseColor("#7B8796"));
        footerPaint.setTextSize(8.5f);
        float footerY = pageInfo.getPageHeight() - 28;
        canvas.drawText("Generated via " + resolveCompanyName(), contentLeft, footerY, footerPaint);
        canvas.drawText("Page 1 of 1", (contentLeft + contentRight) / 2f - 18, footerY, footerPaint);
        canvas.drawText("Secure PDF Document", contentRight - 110, footerY, footerPaint);

        document.finishPage(page);

        String fileName = "Worker_Report_" + System.currentTimeMillis() + ".pdf";
        boolean success = savePdfToDownloads(document, fileName);
        document.close();

        if (success) {
            NotificationStore.pushNotification(this, "Report Downloaded", "Worker report saved as " + fileName);
            Toast.makeText(this, getString(R.string.pdf_saved_success, fileName), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.failed_to_save_pdf, Toast.LENGTH_SHORT).show();
        }
    }

    private void drawLabelValueBlock(
            Canvas canvas,
            float x,
            float y,
            String label,
            String value,
            Paint labelPaint,
            Paint valuePaint
    ) {
        String[] labelLines = safeText(label, "-").split("\n");
        String[] valueLines = safeText(value, "-").split("\n");
        float labelY = y;
        for (String line : labelLines) {
            canvas.drawText(line, x, labelY, labelPaint);
            labelY += 12;
        }

        float valueX = x + 52;
        float valueY = y;
        for (String line : valueLines) {
            canvas.drawText(line, valueX, valueY, valuePaint);
            valueY += 12;
        }
    }

    private void drawSummaryLine(
            Canvas canvas,
            float left,
            float right,
            float y,
            String label,
            String value,
            Paint labelPaint,
            Paint valuePaint,
            boolean deduction
    ) {
        canvas.drawText(label, left, y, labelPaint);
        Paint valuePaintLocal = new Paint(valuePaint);
        valuePaintLocal.setTextAlign(Paint.Align.RIGHT);
        valuePaintLocal.setColor(deduction ? Color.parseColor("#DC2626") : valuePaint.getColor());
        canvas.drawText(value, right, y, valuePaintLocal);
    }

    private String buildReportId() {
        return "AR-" + new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new Date());
    }

    private double parseAmount(String raw) {
        if (raw == null) return 0d;
        String clean = raw.replaceAll("[^0-9.\\-]", "");
        if (clean.trim().isEmpty()) return 0d;
        try {
            return Double.parseDouble(clean);
        } catch (Exception ignored) {
            return 0d;
        }
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
