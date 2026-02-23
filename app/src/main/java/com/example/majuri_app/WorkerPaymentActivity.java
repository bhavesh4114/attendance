package com.example.majuri_app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class WorkerPaymentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_payment);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}
