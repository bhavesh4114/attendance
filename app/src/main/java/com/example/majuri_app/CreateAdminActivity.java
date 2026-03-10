package com.example.majuri_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class CreateAdminActivity extends AppCompatActivity {

    private boolean passwordVisible = false;
    private MaterialButton btnCreateAccount;
    private CheckBox checkTerms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_admin);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        ImageView btnTogglePassword = findViewById(R.id.btnTogglePassword);
        if (btnTogglePassword != null) {
            btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility(btnTogglePassword));
        }

        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        if (btnCreateAccount != null) {
            btnCreateAccount.setOnClickListener(v -> onCreateAccountClick());
        }

        checkTerms = findViewById(R.id.checkTerms);
        bindTermsApprovalControls();

        View btnLoginLink = findViewById(R.id.btnLoginLink);
        if (btnLoginLink != null) {
            btnLoginLink.setOnClickListener(v -> goToLogin());
        }

        setupTermsClickableSpans();
    }

    private void bindTermsApprovalControls() {
        View rowTermsSection = findViewById(R.id.rowTermsSection);

        if (checkTerms != null) {
            checkTerms.setOnCheckedChangeListener((buttonView, isChecked) -> updateCreateAccountButtonState(isChecked));
        }

        if (rowTermsSection != null) {
            rowTermsSection.setOnClickListener(v -> {
                if (checkTerms != null) {
                    checkTerms.toggle();
                }
            });
        }

        updateCreateAccountButtonState(checkTerms != null && checkTerms.isChecked());
    }

    private void updateCreateAccountButtonState(boolean termsApproved) {
        if (btnCreateAccount == null) return;
        btnCreateAccount.setEnabled(termsApproved);
        btnCreateAccount.setAlpha(termsApproved ? 1f : 0.6f);
    }

    private void togglePasswordVisibility(ImageView btnTogglePassword) {
        passwordVisible = !passwordVisible;
        View parent = (View) btnTogglePassword.getParent();
        if (parent != null) {
            EditText etPassword = parent.findViewById(R.id.etPassword);
            if (etPassword != null) {
                if (passwordVisible) {
                    etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    btnTogglePassword.setImageResource(R.drawable.ic_eye);
                } else {
                    etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    btnTogglePassword.setImageResource(R.drawable.ic_eye_off);
                }
            }
        }
    }

    private void onCreateAccountClick() {
        EditText etFullName = findViewById(R.id.etFullName);
        EditText etMobile = findViewById(R.id.etMobile);
        EditText etBusinessName = findViewById(R.id.etBusinessName);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        CheckBox checkTerms = findViewById(R.id.checkTerms);

        String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String mobile = etMobile.getText() != null ? etMobile.getText().toString().trim() : "";
        String businessName = etBusinessName.getText() != null ? etBusinessName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        etFullName.setError(null);
        etMobile.setError(null);
        etBusinessName.setError(null);
        etEmail.setError(null);
        etPassword.setError(null);

        if (fullName.isEmpty()) {
            etFullName.setError(getString(R.string.hint_full_name));
            etFullName.requestFocus();
            return;
        }
        if (fullName.length() < 3) {
            etFullName.setError(getString(R.string.error_invalid_full_name));
            etFullName.requestFocus();
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

        if (businessName.isEmpty()) {
            etBusinessName.setError(getString(R.string.error_enter_business_name));
            etBusinessName.requestFocus();
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
        if (checkTerms == null || !checkTerms.isChecked()) {
            Toast.makeText(this, R.string.agree_terms_hint, Toast.LENGTH_SHORT).show();
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

        builderDb.insertBuilder(fullName, digitsOnly, businessName, email, password);
        builderDb.close();

        Toast.makeText(this, R.string.registration_successful, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void setupTermsClickableSpans() {
        android.widget.TextView tvTerms = findViewById(R.id.tvTerms);
        if (tvTerms == null) return;

        String fullText = getString(R.string.terms_text);
        String terms = "Terms of Service";
        String privacy = "Privacy Policy";

        SpannableString spannable = new SpannableString(fullText);
        int color = getResources().getColor(R.color.light_blue_text, getTheme());

        int termsStart = fullText.indexOf(terms);
        if (termsStart >= 0) {
            int termsEnd = termsStart + terms.length();
            spannable.setSpan(new ForegroundColorSpan(color), termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    Toast.makeText(CreateAdminActivity.this, "Terms of Service", Toast.LENGTH_SHORT).show();
                }
            }, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        int privacyStart = fullText.indexOf(privacy);
        if (privacyStart >= 0) {
            int privacyEnd = privacyStart + privacy.length();
            spannable.setSpan(new ForegroundColorSpan(color), privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    Toast.makeText(CreateAdminActivity.this, "Privacy Policy", Toast.LENGTH_SHORT).show();
                }
            }, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        tvTerms.setText(spannable);
        tvTerms.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
