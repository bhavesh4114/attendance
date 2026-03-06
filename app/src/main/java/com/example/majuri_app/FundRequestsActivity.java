package com.example.majuri_app;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FundRequestsActivity extends AppCompatActivity {

    private WorkerDbHelper workerDbHelper;
    private BuilderDbHelper builderDbHelper;
    private SessionManager sessionManager;

    private EditText etAmount;
    private EditText etOptionalNote;
    private Spinner spinnerCompanyName;
    private LinearLayout historyContainer;
    private TextView tvNoHistory;

    private final DecimalFormat amountFormat = new DecimalFormat("#,##0.00");
    private final SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);

    private String companyPlaceholder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fund_requests);

        workerDbHelper = new WorkerDbHelper(this);
        builderDbHelper = new BuilderDbHelper(this);
        sessionManager = new SessionManager(this);

        etAmount = findViewById(R.id.etAmount);
        etOptionalNote = findViewById(R.id.etOptionalNote);
        spinnerCompanyName = findViewById(R.id.spinnerCompanyName);
        historyContainer = findViewById(R.id.historyContainer);
        tvNoHistory = findViewById(R.id.tvNoHistory);
        companyPlaceholder = getString(R.string.choose_a_builder);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSubmitRequest).setOnClickListener(v -> submitFundRequest());

        setupCompanyDropdown();
        loadRequestHistory();
    }

    private void setupCompanyDropdown() {
        List<String> companies = new ArrayList<>();
        companies.add(companyPlaceholder);
        try {
            List<String> fromDb = builderDbHelper.getAllBusinessNames();
            if (fromDb != null) {
                companies.addAll(fromDb);
            }
        } catch (Exception ignored) {
            // Leave placeholder-only dropdown on DB error.
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                companies
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCompanyName.setAdapter(adapter);
    }

    private void submitFundRequest() {
        String amountText = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
        String companyName = spinnerCompanyName.getSelectedItem() != null
                ? String.valueOf(spinnerCompanyName.getSelectedItem()).trim()
                : "";
        String note = etOptionalNote.getText() != null ? etOptionalNote.getText().toString().trim() : "";
        String contractorId = sessionManager.getLoggedInMobile();

        if (TextUtils.isEmpty(amountText)) {
            etAmount.setError(getString(R.string.fund_request_amount_required));
            etAmount.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(companyName) || companyPlaceholder.equalsIgnoreCase(companyName)) {
            Toast.makeText(this, R.string.fund_request_company_required, Toast.LENGTH_SHORT).show();
            return;
        }

        double amountValue;
        try {
            amountValue = Double.parseDouble(amountText);
        } catch (Exception ignored) {
            amountValue = 0d;
        }
        if (amountValue <= 0d) {
            etAmount.setError(getString(R.string.fund_request_amount_required));
            etAmount.requestFocus();
            return;
        }

        long insertId = workerDbHelper.insertFundRequest(contractorId, companyName, amountValue, note);
        if (insertId > 0L) {
            Toast.makeText(this, R.string.fund_request_submit_success, Toast.LENGTH_SHORT).show();
            clearForm();
            loadRequestHistory();
        } else {
            Toast.makeText(this, R.string.fund_request_submit_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void clearForm() {
        etAmount.setText("");
        etAmount.setError(null);
        etOptionalNote.setText("");
        spinnerCompanyName.setSelection(0);
    }

    private void loadRequestHistory() {
        if (historyContainer == null) return;

        historyContainer.removeAllViews();
        String contractorId = sessionManager.getLoggedInMobile();
        List<FundRequestRecord> requests = workerDbHelper.getFundRequestsForContractor(contractorId);

        if (requests == null || requests.isEmpty()) {
            if (tvNoHistory != null) {
                tvNoHistory.setVisibility(View.VISIBLE);
            }
            return;
        }
        if (tvNoHistory != null) {
            tvNoHistory.setVisibility(View.GONE);
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (FundRequestRecord request : requests) {
            View itemView = inflater.inflate(R.layout.item_fund_request_history, historyContainer, false);

            TextView tvCompany = itemView.findViewById(R.id.tvCompanyName);
            TextView tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
            TextView tvAmount = itemView.findViewById(R.id.tvAmount);
            TextView tvStatus = itemView.findViewById(R.id.tvStatus);

            tvCompany.setText(request.getCompanyName());
            tvCreatedAt.setText(formatDateForHistory(request.getCreatedAt()));
            tvAmount.setText(getString(R.string.fund_currency_amount_format, amountFormat.format(request.getAmount())));
            tvStatus.setText(request.getStatus());

            historyContainer.addView(itemView);
        }
    }

    private String formatDateForHistory(String dbDateText) {
        if (dbDateText == null || dbDateText.trim().isEmpty()) {
            return "";
        }
        try {
            Date parsed = dbDateFormat.parse(dbDateText.trim());
            if (parsed == null) return dbDateText;
            return displayDateFormat.format(parsed);
        } catch (Exception ignored) {
            return dbDateText;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (workerDbHelper != null) {
            workerDbHelper.close();
        }
        if (builderDbHelper != null) {
            builderDbHelper.close();
        }
    }
}
