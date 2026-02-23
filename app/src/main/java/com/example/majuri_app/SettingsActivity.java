package com.example.majuri_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends AppCompatActivity {

    private void doLogout() {
        new SessionManager(this).clearSession();
        Toast.makeText(this, R.string.logout, Toast.LENGTH_SHORT).show();
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.rowChangePassword).setOnClickListener(v ->
                Toast.makeText(this, R.string.change_password, Toast.LENGTH_SHORT).show());
        findViewById(R.id.rowBackupData).setOnClickListener(v ->
                Toast.makeText(this, R.string.backup_data, Toast.LENGTH_SHORT).show());
        findViewById(R.id.rowLanguage).setOnClickListener(v ->
                Toast.makeText(this, R.string.language, Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnLogout).setOnClickListener(v -> doLogout());

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            }
            if (id == R.id.nav_staff) {
                startActivity(new Intent(this, WorkersListActivity.class));
                finish();
                return true;
            }
            if (id == R.id.nav_tasks) {
                startActivity(new Intent(this, AttendanceManagementActivity.class));
                finish();
                return true;
            }
            if (id == R.id.nav_settings) {
                return true;
            }
            return false;
        });
    }
}
