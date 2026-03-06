package com.example.majuri_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FundsActivity extends AppCompatActivity {

    private WorkerDbHelper workerDbHelper;
    private SessionManager sessionManager;

    private TextView tvAllRequestsCount;
    private TextView tvNoPendingRequest;
    private LinearLayout pendingRequestsContainer;
    private TextView tvNoTransactionHistory;
    private LinearLayout transactionHistoryContainer;

    private final DecimalFormat amountFormat = new DecimalFormat("#,##0.00");
    private final SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_funds);

        workerDbHelper = new WorkerDbHelper(this);
        sessionManager = new SessionManager(this);

        tvAllRequestsCount = findViewById(R.id.tvAllRequestsCount);
        tvNoPendingRequest = findViewById(R.id.tvNoPendingRequest);
        pendingRequestsContainer = findViewById(R.id.pendingRequestsContainer);
        tvNoTransactionHistory = findViewById(R.id.tvNoTransactionHistory);
        transactionHistoryContainer = findViewById(R.id.transactionHistoryContainer);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            startActivity(new Intent(this, UserDashboardActivity.class));
            finish();
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_user_payslips);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_user_home) {
                startActivity(new Intent(this, UserDashboardActivity.class));
                finish();
                return true;
            }
            if (id == R.id.nav_user_workers) {
                Intent intent = new Intent(this, WorkersListActivity.class);
                intent.putExtra(WorkersListActivity.EXTRA_FORCE_USER_FLOW, true);
                startActivity(intent);
                finish();
                return true;
            }
            if (id == R.id.nav_user_payslips) {
                return true;
            }
            if (id == R.id.nav_user_profile) {
                startActivity(new Intent(this, BuilderSettingsActivity.class));
                finish();
                return true;
            }
            return false;
        });

        loadAllFundRequests();
        loadTransactionHistory();
    }

    private void loadAllFundRequests() {
        String contractorId = sessionManager.getLoggedInMobile();
        List<FundRequestRecord> allRequests = workerDbHelper.getFundRequestsForContractor(contractorId);
        if (allRequests == null || allRequests.isEmpty()) {
            allRequests = workerDbHelper.getAllFundRequests();
        }

        java.util.ArrayList<FundRequestRecord> requests = new java.util.ArrayList<>();
        if (allRequests != null) {
            for (FundRequestRecord item : allRequests) {
                if (item != null && "Pending".equalsIgnoreCase(item.getStatus())) {
                    requests.add(item);
                }
            }
        }

        int total = requests.size();
        if (tvAllRequestsCount != null) {
            tvAllRequestsCount.setText(getString(R.string.all_requests_count_dynamic, total));
        }

        if (pendingRequestsContainer != null) {
            pendingRequestsContainer.removeAllViews();
        }

        if (requests.isEmpty()) {
            showEmptyPendingState();
            return;
        }

        if (tvNoPendingRequest != null) {
            tvNoPendingRequest.setVisibility(View.GONE);
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (FundRequestRecord request : requests) {
            if (request == null) continue;
            View item = inflater.inflate(R.layout.item_pending_request, pendingRequestsContainer, false);

            TextView tvCompany = item.findViewById(R.id.tvPendingCompanyName);
            TextView tvProject = item.findViewById(R.id.tvPendingProject);
            TextView tvAmount = item.findViewById(R.id.tvPendingAmount);
            TextView tvRequestedOn = item.findViewById(R.id.tvPendingRequestedOn);
            TextView tvNote = item.findViewById(R.id.tvPendingNote);
            View btnReject = item.findViewById(R.id.btnReject);
            View btnApprove = item.findViewById(R.id.btnApprove);

            tvCompany.setText(request.getCompanyName());
            tvAmount.setText(getString(R.string.fund_currency_amount_format, amountFormat.format(request.getAmount())));
            tvRequestedOn.setText(getString(R.string.requested_on_date_format, formatDate(request.getCreatedAt())));

            String note = request.getNote();
            tvNote.setText((note == null || note.trim().isEmpty())
                    ? getString(R.string.fund_request_note_empty)
                    : note.trim());

            String requestContractorId = request.getContractorId();
            String suffix = requestContractorId != null && requestContractorId.length() >= 4
                    ? requestContractorId.substring(requestContractorId.length() - 4)
                    : requestContractorId;
            tvProject.setText(getString(R.string.project_id_format, suffix != null ? suffix : ""));

            btnReject.setOnClickListener(v ->
                    Toast.makeText(this, R.string.reject, Toast.LENGTH_SHORT).show());
            btnApprove.setOnClickListener(v -> {
                Intent intent = new Intent(this, ApprovePaymentActivity.class);
                intent.putExtra(ApprovePaymentActivity.EXTRA_FUND_REQUEST_ID, request.getId());
                startActivity(intent);
            });

            if (pendingRequestsContainer != null) {
                pendingRequestsContainer.addView(item);
            }
        }
    }

    private void loadTransactionHistory() {
        if (transactionHistoryContainer != null) {
            transactionHistoryContainer.removeAllViews();
        }

        List<FundRequestRecord> historyItems = workerDbHelper.getFundTransactionHistory();
        if (historyItems == null || historyItems.isEmpty()) {
            if (tvNoTransactionHistory != null) {
                tvNoTransactionHistory.setVisibility(View.VISIBLE);
            }
            return;
        }
        if (tvNoTransactionHistory != null) {
            tvNoTransactionHistory.setVisibility(View.GONE);
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (FundRequestRecord item : historyItems) {
            if (item == null) continue;
            View row = inflater.inflate(R.layout.item_fund_transaction_history, transactionHistoryContainer, false);
            TextView tvTitle = row.findViewById(R.id.tvTransactionTitle);
            TextView tvDate = row.findViewById(R.id.tvTransactionDate);
            TextView tvAmount = row.findViewById(R.id.tvTransactionAmount);
            View statusIconBg = row.findViewById(R.id.statusIconBg);
            ImageView statusIcon = row.findViewById(R.id.statusIcon);

            boolean isApproved = "Approved".equalsIgnoreCase(item.getStatus());
            tvTitle.setText(isApproved ? getString(R.string.funds_approved_credit) : getString(R.string.funds_rejected_refund));
            tvDate.setText(formatDate(item.getCreatedAt()));
            tvAmount.setText(getString(R.string.fund_currency_amount_format, amountFormat.format(item.getAmount())));

            if (isApproved) {
                statusIconBg.setBackgroundResource(R.drawable.bg_soft_circle_green);
                statusIcon.setImageResource(R.drawable.ic_check_green);
            } else {
                statusIconBg.setBackgroundResource(R.drawable.bg_badge_red);
                statusIcon.setImageResource(R.drawable.ic_close_red);
            }

            if (transactionHistoryContainer != null) {
                transactionHistoryContainer.addView(row);
            }
        }
    }

    private void showEmptyPendingState() {
        if (tvNoPendingRequest != null) {
            tvNoPendingRequest.setVisibility(View.VISIBLE);
        }
    }

    private String formatDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        try {
            Date parsed = dbDateFormat.parse(raw.trim());
            return parsed == null ? raw : displayDateFormat.format(parsed);
        } catch (Exception ignored) {
            return raw;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (workerDbHelper != null) {
            workerDbHelper.close();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllFundRequests();
        loadTransactionHistory();
    }
}
