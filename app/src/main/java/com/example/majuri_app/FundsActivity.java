package com.example.majuri_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class FundsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_funds);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            startActivity(new Intent(this, UserDashboardActivity.class));
            finish();
        });

        findViewById(R.id.btnReject).setOnClickListener(v ->
                Toast.makeText(this, R.string.reject, Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnApprove).setOnClickListener(v ->
                startActivity(new Intent(this, ApprovePaymentActivity.class)));

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
                Toast.makeText(this, R.string.nav_profile, Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }
}
