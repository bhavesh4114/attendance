package com.example.majuri_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class BuilderSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_builder_settings);

        bindProfileDetails();
        bindActions();
    }

    private void bindProfileDetails() {
        SessionManager sessionManager = new SessionManager(this);
        String loginId = sessionManager.getLoggedInMobile();
        String userName = sessionManager.getLoggedInUserName();

        BuilderDbHelper builderDbHelper = new BuilderDbHelper(this);
        String registeredName = builderDbHelper.getBuilderDisplayName(loginId);
        builderDbHelper.close();

        if (registeredName != null && !registeredName.trim().isEmpty()) {
            userName = registeredName.trim();
        }

        if (userName == null || userName.trim().isEmpty()) {
            userName = getString(R.string.builder_name_fallback);
        }

        TextView tvBuilderName = findViewById(R.id.tvBuilderName);
        TextView tvBuilderId = findViewById(R.id.tvBuilderId);
        tvBuilderName.setText(userName.trim());
        tvBuilderId.setText(getString(R.string.builder_id_format, resolveBuilderCode(loginId)));
    }

    private String resolveBuilderCode(String mobile) {
        if (mobile == null) {
            return "1024";
        }
        String digits = mobile.replaceAll("[^0-9]", "");
        if (digits.length() >= 4) {
            return digits.substring(digits.length() - 4);
        }
        if (!digits.isEmpty()) {
            return String.format(Locale.US, "%04d", Integer.parseInt(digits));
        }
        return "1024";
    }

    private void bindActions() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnBell).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationActivity.class)));
        findViewById(R.id.tvEditProfile).setOnClickListener(v ->
                Toast.makeText(this, R.string.edit, Toast.LENGTH_SHORT).show());
        findViewById(R.id.rowChangePassword).setOnClickListener(v ->
                Toast.makeText(this, R.string.change_password, Toast.LENGTH_SHORT).show());
        findViewById(R.id.rowLanguage).setOnClickListener(v ->
                Toast.makeText(this, R.string.language, Toast.LENGTH_SHORT).show());
        findViewById(R.id.rowNotifications).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationActivity.class)));
        findViewById(R.id.btnLogout).setOnClickListener(v -> logoutAndOpenLogin());
    }

    private void logoutAndOpenLogin() {
        new SessionManager(this).clearSession();
        Toast.makeText(this, R.string.logout, Toast.LENGTH_SHORT).show();
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
