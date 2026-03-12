package com.example.majuri_app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class CreateAdminActivity extends AppCompatActivity {

    private EditText etCompanyName;
    private EditText etBuilderFullName;
    private EditText etMobile;
    private EditText etEmail;
    private EditText etSiteName;
    private EditText etSiteAddress;
    private EditText etCity;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private MaterialButton btnCreateAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_admin);

        bindViews();

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnCreateAccount != null) {
            btnCreateAccount.setOnClickListener(v -> onCreateAccountClick());
        }

        TextView btnLoginLink = findViewById(R.id.btnLoginLink);
        if (btnLoginLink != null) {
            btnLoginLink.setOnClickListener(v -> goToLogin());
        }
    }

    private void bindViews() {
        etCompanyName = findViewById(R.id.etCompanyName);
        etBuilderFullName = findViewById(R.id.etBuilderFullName);
        etMobile = findViewById(R.id.etMobile);
        etEmail = findViewById(R.id.etEmail);
        etSiteName = findViewById(R.id.etSiteName);
        etSiteAddress = findViewById(R.id.etSiteAddress);
        etCity = findViewById(R.id.etCity);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
    }

    private void onCreateAccountClick() {
        String companyName = readTrim(etCompanyName);
        String builderFullName = readTrim(etBuilderFullName);
        String mobile = readTrim(etMobile);
        String email = readTrim(etEmail);
        String siteName = readTrim(etSiteName);
        String siteAddress = readTrim(etSiteAddress);
        String city = readTrim(etCity);
        String password = readRaw(etPassword);
        String confirmPassword = readRaw(etConfirmPassword);

        clearErrors();

        if (companyName.isEmpty()) {
            etCompanyName.setError(getString(R.string.error_enter_company_name));
            etCompanyName.requestFocus();
            return;
        }
        if (builderFullName.isEmpty()) {
            etBuilderFullName.setError(getString(R.string.error_enter_builder_name));
            etBuilderFullName.requestFocus();
            return;
        }
        if (builderFullName.length() < 3) {
            etBuilderFullName.setError(getString(R.string.error_invalid_full_name));
            etBuilderFullName.requestFocus();
            return;
        }

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

        if (email.isEmpty()) {
            etEmail.setError(getString(R.string.error_enter_email));
            etEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.error_invalid_email));
            etEmail.requestFocus();
            return;
        }

        if (siteName.isEmpty()) {
            etSiteName.setError(getString(R.string.error_enter_site_name));
            etSiteName.requestFocus();
            return;
        }

        if (siteAddress.isEmpty()) {
            etSiteAddress.setError(getString(R.string.error_enter_site_address));
            etSiteAddress.requestFocus();
            return;
        }

        if (city.isEmpty()) {
            etCity.setError(getString(R.string.error_enter_city));
            etCity.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError(getString(R.string.error_enter_password));
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError(getString(R.string.error_password_min_length));
            etPassword.requestFocus();
            return;
        }

        if (confirmPassword.isEmpty()) {
            etConfirmPassword.setError(getString(R.string.error_enter_confirm_password));
            etConfirmPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError(getString(R.string.error_password_confirm_mismatch));
            etConfirmPassword.requestFocus();
            return;
        }

        BuilderDbHelper builderDb = new BuilderDbHelper(this);
        boolean exists = builderDb.isBuilderExists(digitsOnly);
        if (exists) {
            builderDb.close();
            Toast.makeText(this, R.string.mobile_already_registered, Toast.LENGTH_LONG).show();
            goToLogin();
            return;
        }

        long rowId = builderDb.insertBuilder(
                builderFullName,
                digitsOnly,
                companyName,
                email,
                siteName,
                siteAddress,
                city,
                password
        );
        builderDb.close();

        if (rowId <= 0) {
            Toast.makeText(this, R.string.error_registration_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.registration_successful, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void clearErrors() {
        etCompanyName.setError(null);
        etBuilderFullName.setError(null);
        etMobile.setError(null);
        etEmail.setError(null);
        etSiteName.setError(null);
        etSiteAddress.setError(null);
        etCity.setError(null);
        etPassword.setError(null);
        etConfirmPassword.setError(null);
    }

    private String readTrim(EditText input) {
        return input != null && input.getText() != null ? input.getText().toString().trim() : "";
    }

    private String readRaw(EditText input) {
        return input != null && input.getText() != null ? input.getText().toString() : "";
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
