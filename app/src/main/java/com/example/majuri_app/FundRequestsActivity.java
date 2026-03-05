package com.example.majuri_app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class FundRequestsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fund_requests);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}
