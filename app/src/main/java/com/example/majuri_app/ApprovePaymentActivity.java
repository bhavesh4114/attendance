package com.example.majuri_app;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class ApprovePaymentActivity extends AppCompatActivity {

    private View optionUpi;
    private View optionCash;
    private View radioUpi;
    private View radioCash;
    private MaterialButton btnConfirmPayment;
    private boolean isUpiSelected = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_approve_payment);

        bindViews();
        bindListeners();
        updatePaymentMethodUi();
    }

    private void bindViews() {
        optionUpi = findViewById(R.id.optionUpiPayment);
        optionCash = findViewById(R.id.optionCashPayment);
        radioUpi = findViewById(R.id.radioUpi);
        radioCash = findViewById(R.id.radioCash);
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment);
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
            btnConfirmPayment.setOnClickListener(v -> {
                String selectedMethod = isUpiSelected ? "UPI" : "Cash";
                Toast.makeText(this, getString(R.string.confirmed_payment_method, selectedMethod), Toast.LENGTH_SHORT).show();
            });
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
        TextView tvTransactionId = findViewById(R.id.tvTransactionId);
        if (tvTransactionId != null) {
            tvTransactionId.setText(getString(R.string.transaction_id_fund));
        }
    }
}
