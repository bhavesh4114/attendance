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

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ApprovePaymentActivity extends AppCompatActivity implements PaymentResultListener {

    public static final String EXTRA_FUND_REQUEST_ID = "extra_fund_request_id";

    private View optionUpi;
    private View optionCash;
    private View radioUpi;
    private View radioCash;
    private MaterialButton btnConfirmPayment;
    private TextView tvRequestedAmountValue;
    private TextView tvContractorValue;
    private TextView tvDateRequestedValue;
    private TextView tvPaymentNoteValue;
    private TextView tvTotalPayableValue;
    private TextView tvTransactionId;

    private WorkerDbHelper workerDbHelper;
    private SessionManager sessionManager;
    private FundRequestRecord selectedRequest;
    private boolean isUpiSelected = true;
    private final DecimalFormat amountFormat = new DecimalFormat("#,##0.00");
    private final SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_approve_payment);

        workerDbHelper = new WorkerDbHelper(this);
        sessionManager = new SessionManager(this);
        Checkout.preload(getApplicationContext());

        bindViews();
        loadPendingRequestData();
        bindListeners();
        updatePaymentMethodUi();
    }

    private void bindViews() {
        optionUpi = findViewById(R.id.optionUpiPayment);
        optionCash = findViewById(R.id.optionCashPayment);
        radioUpi = findViewById(R.id.radioUpi);
        radioCash = findViewById(R.id.radioCash);
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment);
        tvRequestedAmountValue = findViewById(R.id.tvRequestedAmountValue);
        tvContractorValue = findViewById(R.id.tvContractorValue);
        tvDateRequestedValue = findViewById(R.id.tvDateRequestedValue);
        tvPaymentNoteValue = findViewById(R.id.tvPaymentNoteValue);
        tvTotalPayableValue = findViewById(R.id.tvTotalPayableValue);
        tvTransactionId = findViewById(R.id.tvTransactionId);
    }

    private void loadPendingRequestData() {
        long requestId = getIntent().getLongExtra(EXTRA_FUND_REQUEST_ID, -1L);
        if (requestId > 0L) {
            selectedRequest = workerDbHelper.getFundRequestById(requestId);
        }

        if (selectedRequest == null) {
            String contractorId = sessionManager.getLoggedInMobile();
            List<FundRequestRecord> requests = workerDbHelper.getFundRequestsForContractor(contractorId);
            if (requests == null || requests.isEmpty()) {
                requests = workerDbHelper.getAllFundRequests();
            }
            if (requests != null) {
                for (FundRequestRecord item : requests) {
                    if (item != null && "Pending".equalsIgnoreCase(item.getStatus())) {
                        selectedRequest = item;
                        break;
                    }
                }
                if (selectedRequest == null && !requests.isEmpty()) {
                    selectedRequest = requests.get(0);
                }
            }
        }

        if (selectedRequest == null) {
            return;
        }

        String amount = getString(R.string.fund_currency_amount_format, amountFormat.format(selectedRequest.getAmount()));
        String note = selectedRequest.getNote() != null ? selectedRequest.getNote().trim() : "";
        if (note.isEmpty()) {
            note = getString(R.string.fund_request_note_empty);
        }

        if (tvRequestedAmountValue != null) {
            tvRequestedAmountValue.setText(amount);
        }
        if (tvTotalPayableValue != null) {
            tvTotalPayableValue.setText(amount);
        }
        if (tvContractorValue != null) {
            tvContractorValue.setText(selectedRequest.getContractorId());
        }
        if (tvDateRequestedValue != null) {
            tvDateRequestedValue.setText(formatDate(selectedRequest.getCreatedAt()));
        }
        if (tvPaymentNoteValue != null) {
            tvPaymentNoteValue.setText(note);
        }
        if (tvTransactionId != null) {
            tvTransactionId.setText(getString(R.string.transaction_id_fund_format, selectedRequest.getId()));
        }
    }

    private void bindListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        if (optionUpi != null) {
            optionUpi.setOnClickListener(v -> {
                isUpiSelected = true;
                updatePaymentMethodUi();
            });
        }
        if (optionCash != null) {
            optionCash.setOnClickListener(v -> {
                isUpiSelected = false;
                updatePaymentMethodUi();
            });
        }
        if (btnConfirmPayment != null) {
            btnConfirmPayment.setOnClickListener(v -> handleConfirmPayment());
        }
    }

    private void handleConfirmPayment() {
        if (selectedRequest == null) {
            Toast.makeText(this, R.string.fund_request_submit_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        if (isUpiSelected) {
            startRazorpayCheckout();
            return;
        }

        boolean saved = workerDbHelper.approveFundRequest(selectedRequest.getId(), "Cash", "");
        if (saved) {
            Toast.makeText(this, R.string.payment_successful, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, R.string.payment_record_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void startRazorpayCheckout() {
        if (selectedRequest == null) {
            Toast.makeText(this, R.string.fund_request_submit_failed, Toast.LENGTH_SHORT).show();
            return;
        }

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
            options.put("description", "Fund approval for " + selectedRequest.getCompanyName());
            options.put("currency", "INR");
            options.put("amount", (long) Math.round(selectedRequest.getAmount() * 100d));
            checkout.open(this, options);
        } catch (Exception e) {
            Toast.makeText(this, R.string.payment_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPaymentSuccess(String razorpayPaymentId) {
        if (selectedRequest == null) {
            Toast.makeText(this, R.string.payment_record_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        String ref = razorpayPaymentId != null ? razorpayPaymentId.trim() : "";
        boolean saved = workerDbHelper.approveFundRequest(selectedRequest.getId(), "Online", ref);
        if (saved) {
            Toast.makeText(this, R.string.payment_successful, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, R.string.payment_record_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPaymentError(int code, String response) {
        Toast.makeText(this, R.string.payment_failed, Toast.LENGTH_SHORT).show();
    }

    private String formatDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "";
        try {
            Date parsed = dbDateFormat.parse(raw.trim());
            return parsed == null ? raw : displayDateFormat.format(parsed);
        } catch (Exception ignored) {
            return raw;
        }
    }

    private void updatePaymentMethodUi() {
        if (optionUpi != null) {
            optionUpi.setBackgroundResource(isUpiSelected
                    ? R.drawable.bg_payment_method_selected_card
                    : R.drawable.bg_payment_method_unselected_card);
        }
        if (optionCash != null) {
            optionCash.setBackgroundResource(isUpiSelected
                    ? R.drawable.bg_payment_method_unselected_card
                    : R.drawable.bg_payment_method_selected_card);
        }
        if (radioUpi != null) {
            radioUpi.setBackgroundResource(isUpiSelected
                    ? R.drawable.bg_method_radio_selected
                    : R.drawable.bg_method_radio_unselected);
        }
        if (radioCash != null) {
            radioCash.setBackgroundResource(isUpiSelected
                    ? R.drawable.bg_method_radio_unselected
                    : R.drawable.bg_method_radio_selected);
        }
        if (tvTransactionId != null && selectedRequest == null) {
            tvTransactionId.setText(getString(R.string.transaction_id_fund));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (workerDbHelper != null) {
            workerDbHelper.close();
        }
    }
}
