package com.example.majuri_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class LoginActivity extends AppCompatActivity {

    private EditText etMobile, etPassword;
    private SessionManager sessionManager;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        etMobile = findViewById(R.id.etMobile);
        etPassword = findViewById(R.id.etPassword);
        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        TextView btnRegister = findViewById(R.id.btnRegister);
        TextView btnForgotPassword = findViewById(R.id.btnForgotPassword);
        TextView btnShowPassword = findViewById(R.id.btnShowPassword);

        btnLogin.setOnClickListener(v -> onLoginClick());
        btnRegister.setOnClickListener(v -> openSignUp());
        btnForgotPassword.setOnClickListener(v ->
                Toast.makeText(this, R.string.forgot_password, Toast.LENGTH_SHORT).show());

        btnShowPassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            int type = passwordVisible
                    ? android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;
            etPassword.setInputType(type);
            btnShowPassword.setText(passwordVisible ? getString(R.string.hide_password) : getString(R.string.show));
        });
    }

    private void onLoginClick() {
        String mobile = etMobile.getText() != null ? etMobile.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        if (mobile.isEmpty()) {
            etMobile.setError(getString(R.string.error_enter_mobile));
            etMobile.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError(getString(R.string.error_enter_password));
            etPassword.requestFocus();
            return;
        }

        etMobile.setError(null);
        etPassword.setError(null);

        boolean isEmailLogin = mobile.contains("@");
        String normalizedMobile = isEmailLogin ? "" : normalizeMobile(mobile);
        if (!isEmailLogin && normalizedMobile.isEmpty()) {
            etMobile.setError(getString(R.string.error_invalid_mobile));
            etMobile.requestFocus();
            return;
        }

        // 1) Builder/admin credentials from local SQLite.
        BuilderDbHelper builderDb = new BuilderDbHelper(this);
        String builderName = isEmailLogin
                ? builderDb.getBuilderNameIfValidByEmail(mobile, password)
                : builderDb.getBuilderNameIfValid(normalizedMobile, password);
        builderDb.close();

        if (!builderName.isEmpty()) {
            completeLogin(isEmailLogin ? mobile : normalizedMobile, builderName, true);
            return;
        }

        // 2) Worker/user credentials from AuthHelper storage.
        if (!isEmailLogin) {
            AuthHelper authHelper = new AuthHelper(this);
            if (authHelper.validateUser(normalizedMobile, password)) {
                boolean isAdmin = AuthHelper.ROLE_ADMIN.equals(authHelper.getRole(normalizedMobile));
                String userName = authHelper.getUserName(normalizedMobile);
                if (userName == null || userName.trim().isEmpty()) {
                    userName = isAdmin ? "Builder" : "User";
                }
                completeLogin(normalizedMobile, userName, isAdmin);
                return;
            }
        }

        Toast.makeText(this, R.string.error_invalid_credentials, Toast.LENGTH_LONG).show();
    }

    private String normalizeMobile(String value) {
        if (value == null) return "";
        String digitsOnly = value.replaceAll("[^0-9]", "");
        if (digitsOnly.length() < 10) return "";
        return digitsOnly.substring(digitsOnly.length() - 10);
    }

    private void completeLogin(String loginId, String name, boolean isAdmin) {
        sessionManager.saveSession(loginId, name, isAdmin);
        Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
        Intent target = new Intent(this, isAdmin ? DashboardActivity.class : UserDashboardActivity.class);
        startActivity(target);
        finish();
    }

    private void openSignUp() {
        startActivity(new Intent(this, CreateAdminActivity.class));
        finish();
    }
}
