package com.example.majuri_app;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
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

        MaterialButton btnCreateAccount = findViewById(R.id.btnCreateAccount);
        if (btnCreateAccount != null) {
            btnCreateAccount.setOnClickListener(v -> onCreateAccountClick());
        }

        View btnLoginLink = findViewById(R.id.btnLoginLink);
        if (btnLoginLink != null) {
            btnLoginLink.setOnClickListener(v -> finish());
        }

        setupTermsClickableSpans();
    }

    private void togglePasswordVisibility(ImageView btnTogglePassword) {
        passwordVisible = !passwordVisible;
        // Toggle input type and icon - would need to reference etPassword
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
        CheckBox checkTerms = findViewById(R.id.checkTerms);
        if (checkTerms != null && !checkTerms.isChecked()) {
            Toast.makeText(this, R.string.agree_terms_hint, Toast.LENGTH_SHORT).show();
            return;
        }
        // TODO: Validate fields and create account
        Toast.makeText(this, R.string.create_account, Toast.LENGTH_SHORT).show();
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
                    // TODO: Open Terms of Service
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
                    // TODO: Open Privacy Policy
                    Toast.makeText(CreateAdminActivity.this, "Privacy Policy", Toast.LENGTH_SHORT).show();
                }
            }, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        tvTerms.setText(spannable);
        tvTerms.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
