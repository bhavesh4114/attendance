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
    private AuthHelper authHelper;
    private SessionManager sessionManager;
    private boolean passwordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authHelper = new AuthHelper(this);
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
        String digitsOnly = mobile.replaceAll("[^0-9]", "");
        if (digitsOnly.length() != 10) {
            etMobile.setError(getString(R.string.error_invalid_mobile));
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

        if (authHelper.validateUser(mobile, password)) {
            String name = authHelper.getUserName(mobile);
            sessionManager.saveSession(digitsOnly, name);
            Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        } else {
            // No account or wrong password → redirect to Sign up
            Toast.makeText(this, R.string.no_account_sign_up, Toast.LENGTH_LONG).show();
            openSignUp();
        }
    }

    private void openSignUp() {
        startActivity(new Intent(this, CreateAdminActivity.class));
        finish();
    }
}
