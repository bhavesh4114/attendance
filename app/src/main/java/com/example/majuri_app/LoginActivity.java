package com.example.majuri_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import com.example.majuri_app.BuilderDbHelper;

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


        // builder mobile nomber 7600485458
        // builder password builder@123

        // TEMPORARY: test-only builder login override.
        if ("7600485458".equals(mobile) && "builder@123".equals(password)) {
            sessionManager.saveSession("7600485458", "Builder", false);
            Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, UserDashboardActivity.class));
            finish();
            return;
        }

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

        BuilderDbHelper builderDb = new BuilderDbHelper(this);
        String name;
        boolean isEmailLogin = mobile.contains("@");
        if (isEmailLogin) {
            name = builderDb.getBuilderNameIfValidByEmail(mobile, password);
        } else {
            String digitsOnly = mobile.replaceAll("[^0-9]", "");
            if (digitsOnly.length() != 10) {
                etMobile.setError(getString(R.string.error_invalid_mobile));
                etMobile.requestFocus();
                builderDb.close();
                return;
            }
            name = builderDb.getBuilderNameIfValid(digitsOnly, password);
        }
        builderDb.close();

        if (!name.isEmpty()) {
            boolean isAdmin = true;
            sessionManager.saveSession(mobile, name, isAdmin);
            Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
            Intent target = new Intent(this, isAdmin ? DashboardActivity.class : UserDashboardActivity.class);
            startActivity(target);
            finish();
        } else {
            // Wrong mobile / password → stay on login
            Toast.makeText(this, R.string.error_invalid_credentials, Toast.LENGTH_LONG).show();
        }
    }

    private void openSignUp() {
        startActivity(new Intent(this, CreateAdminActivity.class));
        finish();
    }
}
