package com.example.majuri_app;

import android.os.Bundle;
import android.widget.EditText;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.app.DatePickerDialog;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ReportsActivity extends AppCompatActivity {
    private static final String TAG = "ReportsActivity";

    private WorkerDbHelper workerDbHelper;
    private BuilderDbHelper builderDbHelper;
    private Spinner spinnerSelectWorker;
    private Spinner spinnerSkillType;
    private Spinner spinnerSiteName;
    private EditText etFromDate;
    private EditText etToDate;
    private List<WorkerListItem> allWorkers = new ArrayList<>();
    private List<WorkerListItem> displayedWorkers = new ArrayList<>();
    private List<String> allSkillTypes = new ArrayList<>();
    private final Calendar fromDateCalendar = Calendar.getInstance();
    private final Calendar toDateCalendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_report);

        workerDbHelper = new WorkerDbHelper(this);
        builderDbHelper = new BuilderDbHelper(this);
        spinnerSelectWorker = findViewById(R.id.spinnerSelectWorker);
        spinnerSkillType = findViewById(R.id.spinnerSkillType);
        spinnerSiteName = findViewById(R.id.spinnerSiteName);
        etFromDate = findViewById(R.id.etFromDate);
        etToDate = findViewById(R.id.etToDate);

        View backButton = findViewById(R.id.btnBack);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        setupWorkerSpinner();
        setupSiteSpinner();
        setupDatePickers();
    }

    private void setupWorkerSpinner() {
        if (spinnerSelectWorker == null) {
            Log.w(TAG, "Select Worker spinner not found in layout.");
            setupSkillSpinner(new ArrayList<>());
            return;
        }

        allWorkers = getAllWorkers();
        displayedWorkers = new ArrayList<>();
        List<String> workerNames = new ArrayList<>();
        Set<String> skills = new LinkedHashSet<>();
        for (WorkerListItem item : allWorkers) {
            if (item == null) continue;
            if (item.getName() != null && !item.getName().trim().isEmpty()) {
                workerNames.add(item.getName().trim());
                displayedWorkers.add(item);
            }
            if (item.getRole() != null && !item.getRole().trim().isEmpty()) {
                skills.add(item.getRole().trim());
            }
        }
        allSkillTypes = new ArrayList<>(skills);

        if (workerNames.isEmpty()) {
            workerNames = new ArrayList<>();
            workerNames.add(getString(R.string.no_workers_found));
            spinnerSelectWorker.setEnabled(false);
            setupSkillSpinner(new ArrayList<>());
        } else {
            spinnerSelectWorker.setEnabled(true);
            setupSkillSpinner(allSkillTypes);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                workerNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSelectWorker.setAdapter(adapter);

        spinnerSelectWorker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= displayedWorkers.size()) return;
                String selectedSkill = displayedWorkers.get(position).getRole();
                if (selectedSkill == null || selectedSkill.trim().isEmpty() || spinnerSkillType == null) return;
                if (spinnerSkillType.getAdapter() == null) return;

                String normalizedSkill = selectedSkill.trim();
                for (int i = 0; i < spinnerSkillType.getAdapter().getCount(); i++) {
                    Object item = spinnerSkillType.getAdapter().getItem(i);
                    if (item != null && normalizedSkill.equalsIgnoreCase(String.valueOf(item).trim())) {
                        spinnerSkillType.setSelection(i);
                        break;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No-op
            }
        });
    }

    private void setupSkillSpinner(List<String> skills) {
        if (spinnerSkillType == null) {
            Log.w(TAG, "Skill Type spinner not found in layout.");
            return;
        }

        List<String> data = skills;
        if (data == null || data.isEmpty()) {
            data = getAllSkillTypes();
        }

        if (data.isEmpty()) {
            data = new ArrayList<>();
            data.add(getString(R.string.no_skill_types_found));
            spinnerSkillType.setEnabled(false);
        } else {
            spinnerSkillType.setEnabled(true);
        }

        ArrayAdapter<String> skillAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                data
        );
        skillAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSkillType.setAdapter(skillAdapter);
    }

    private void setupSiteSpinner() {
        if (spinnerSiteName == null) {
            Log.w(TAG, "Select Site spinner not found in layout.");
            return;
        }

        List<String> sites = getAllSites();
        if (sites.isEmpty()) {
            sites = new ArrayList<>();
            sites.add(getString(R.string.no_sites_found));
            spinnerSiteName.setEnabled(false);
        } else {
            spinnerSiteName.setEnabled(true);
        }

        ArrayAdapter<String> siteAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                sites
        );
        siteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSiteName.setAdapter(siteAdapter);
    }

    private List<WorkerListItem> getAllWorkers() {
        try {
            return workerDbHelper.getAllWorkers();
        } catch (Exception exception) {
            Log.e(TAG, "Failed to load workers from database.", exception);
            Toast.makeText(this, R.string.unable_to_load_workers, Toast.LENGTH_SHORT).show();
            return new ArrayList<>();
        }
    }

    private List<String> getAllSkillTypes() {
        try {
            return workerDbHelper.getAllSkillTypes();
        } catch (Exception exception) {
            Log.e(TAG, "Failed to load skill types from database.", exception);
            Toast.makeText(this, R.string.unable_to_load_workers, Toast.LENGTH_SHORT).show();
            return new ArrayList<>();
        }
    }

    private List<String> getAllSites() {
        try {
            return builderDbHelper.getAllBusinessNames();
        } catch (Exception exception) {
            Log.e(TAG, "Failed to load site names from database.", exception);
            Toast.makeText(this, R.string.unable_to_load_sites, Toast.LENGTH_SHORT).show();
            return new ArrayList<>();
        }
    }

    private void setupDatePickers() {
        if (etFromDate != null) {
            etFromDate.setOnClickListener(v -> showFromDatePicker());
        }
        if (etToDate != null) {
            etToDate.setOnClickListener(v -> showToDatePicker());
        }
    }

    private void showFromDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    fromDateCalendar.set(year, month, dayOfMonth, 0, 0, 0);
                    fromDateCalendar.set(Calendar.MILLISECOND, 0);
                    if (etFromDate != null) {
                        etFromDate.setText(dateFormat.format(fromDateCalendar.getTime()));
                    }

                    if (toDateCalendar.before(fromDateCalendar)) {
                        toDateCalendar.setTimeInMillis(fromDateCalendar.getTimeInMillis());
                        if (etToDate != null && etToDate.getText() != null && etToDate.getText().length() > 0) {
                            etToDate.setText(dateFormat.format(toDateCalendar.getTime()));
                        }
                    }
                },
                fromDateCalendar.get(Calendar.YEAR),
                fromDateCalendar.get(Calendar.MONTH),
                fromDateCalendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void showToDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    toDateCalendar.set(year, month, dayOfMonth, 0, 0, 0);
                    toDateCalendar.set(Calendar.MILLISECOND, 0);
                    if (etToDate != null) {
                        etToDate.setText(dateFormat.format(toDateCalendar.getTime()));
                    }
                },
                toDateCalendar.get(Calendar.YEAR),
                toDateCalendar.get(Calendar.MONTH),
                toDateCalendar.get(Calendar.DAY_OF_MONTH)
        );

        if (etFromDate != null && etFromDate.getText() != null && etFromDate.getText().length() > 0) {
            dialog.getDatePicker().setMinDate(fromDateCalendar.getTimeInMillis());
        }
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (workerDbHelper != null) {
            workerDbHelper.close();
        }
        if (builderDbHelper != null) {
            builderDbHelper.close();
        }
    }
}
