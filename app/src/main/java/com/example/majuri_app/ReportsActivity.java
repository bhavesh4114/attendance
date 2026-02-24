package com.example.majuri_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ReportsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnMenu).setOnClickListener(v ->
                Toast.makeText(this, R.string.menu, Toast.LENGTH_SHORT).show());

        findViewById(R.id.cardWorkerReport).setOnClickListener(v ->
                Toast.makeText(this, R.string.worker_report, Toast.LENGTH_SHORT).show());
        findViewById(R.id.cardMonthlyReport).setOnClickListener(v ->
                Toast.makeText(this, R.string.monthly_summary, Toast.LENGTH_SHORT).show());
        findViewById(R.id.cardPaymentLogs).setOnClickListener(v ->
                Toast.makeText(this, R.string.payment_logs, Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnDownloadAll).setOnClickListener(v ->
                Toast.makeText(this, R.string.download_all_pdf_reports, Toast.LENGTH_SHORT).show());

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_analytics);
        bottomNav.setOnItemSelectedListener(item -> onNavItemSelected(item.getItemId()));
    }

    private boolean onNavItemSelected(int id) {
        if (id == R.id.nav_dashboard) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return true;
        }
        if (id == R.id.nav_workers) {
            startActivity(new Intent(this, WorkersListActivity.class));
            finish();
            return true;
        }
        if (id == R.id.nav_payments) {
            startActivity(new Intent(this, PaymentsActivity.class));
            finish();
            return true;
        }
        if (id == R.id.nav_analytics) return true;
        if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            finish();
            return true;
        }
        return false;
    }
}
