package com.example.majuri_app;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class AddWorkerActivity extends AppCompatActivity {

    private static final String[] SKILL_CATEGORIES = {
            "Mason",
            "Labour",
            "Electrician",
            "Painter",
            "Plumber",
            "Carpenter",
            "Welder",
            "Other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_worker);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        setupSkillTypeDropdown();

        MaterialButton btnSaveWorker = findViewById(R.id.btnSaveWorker);
        btnSaveWorker.setOnClickListener(v -> onSaveWorker());
    }

    private void setupSkillTypeDropdown() {
        AutoCompleteTextView actvSkillType = findViewById(R.id.actvSkillType);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, SKILL_CATEGORIES);
        actvSkillType.setAdapter(adapter);
        actvSkillType.setOnClickListener(v -> actvSkillType.showDropDown());
    }

    private void onSaveWorker() {
        EditText etWorkerName = findViewById(R.id.etWorkerName);
        EditText etMobileNumber = findViewById(R.id.etMobileNumber);
        EditText etDailyWage = findViewById(R.id.etDailyWage);
        AutoCompleteTextView actvSkillType = findViewById(R.id.actvSkillType);

        String name = etWorkerName.getText() != null ? etWorkerName.getText().toString().trim() : "";
        String mobile = etMobileNumber.getText() != null ? etMobileNumber.getText().toString().trim() : "";
        String wage = etDailyWage.getText() != null ? etDailyWage.getText().toString().trim() : "";
        String skill = actvSkillType.getText() != null ? actvSkillType.getText().toString().trim() : "";

        if (name.isEmpty()) {
            etWorkerName.setError(getString(R.string.hint_enter_full_name));
            return;
        }
        String mobileDigits = mobile.replaceAll("[^0-9]", "");
        if (mobileDigits.length() != 10) {
            etMobileNumber.setError(getString(R.string.hint_10_digit));
            return;
        }
        if (wage.isEmpty()) {
            etDailyWage.setError(getString(R.string.hint_amount_rupee));
            return;
        }
        if (skill.isEmpty()) {
            actvSkillType.setError(getString(R.string.hint_select_category));
            return;
        }

        WorkerDbHelper dbHelper = new WorkerDbHelper(this);
        long rowId = dbHelper.insertWorker(name, mobile, wage, skill);
        dbHelper.close();

        if (rowId != -1) {
            Toast.makeText(this, R.string.add_worker_successfully, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, R.string.save_worker, Toast.LENGTH_SHORT).show();
        }
    }
}
