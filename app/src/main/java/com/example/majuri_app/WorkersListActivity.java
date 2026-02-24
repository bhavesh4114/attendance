package com.example.majuri_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class WorkersListActivity extends AppCompatActivity {

    private WorkersListAdapter adapter;
    private List<WorkerListItem> allItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workers_list_builder);

        loadWorkersFromDb();
        adapter = new WorkersListAdapter();
        if (allItems != null) adapter.setItems(allItems);
        adapter.setOnViewClickListener((item, position) ->
                Toast.makeText(this, getString(R.string.view) + " " + item.getName(), Toast.LENGTH_SHORT).show());

        RecyclerView recyclerWorkers = findViewById(R.id.recyclerWorkers);
        recyclerWorkers.setLayoutManager(new LinearLayoutManager(this));
        recyclerWorkers.setAdapter(adapter);

        EditText searchWorkers = findViewById(R.id.searchWorkers);
        searchWorkers.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterWorkers(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btnNotifications).setOnClickListener(v ->
                Toast.makeText(this, R.string.notifications, Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnFilter).setOnClickListener(v ->
                Toast.makeText(this, R.string.filter, Toast.LENGTH_SHORT).show());

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_user_home);
        bottomNav.setOnItemSelectedListener(item -> onUserNavItemSelected(item.getItemId()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWorkersFromDb();
    }

    private void loadWorkersFromDb() {
        WorkerDbHelper dbHelper = new WorkerDbHelper(this);
        allItems = dbHelper.getAllWorkers();
        dbHelper.close();
        if (adapter != null) {
            adapter.setItems(allItems);
        }
        TextView tvCount = findViewById(R.id.tvActiveWorkersCount);
        if (tvCount != null) {
            tvCount.setText(getString(R.string.active_workers_count_format, allItems != null ? allItems.size() : 0));
        }
    }

    private void filterWorkers(String query) {
        if (query.isEmpty()) {
            adapter.setItems(allItems);
            return;
        }
        if (allItems == null) return;
        String lower = query.toLowerCase();
        List<WorkerListItem> filtered = new ArrayList<>();
        for (WorkerListItem item : allItems) {
            if (item.getName().toLowerCase().contains(lower)
                    || item.getRole().toLowerCase().contains(lower)) {
                filtered.add(item);
            }
        }
        adapter.setItems(filtered);
    }

    private boolean onUserNavItemSelected(int id) {
        if (id == R.id.nav_user_home) {
            startActivity(new Intent(this, UserDashboardActivity.class));
            finish();
            return true;
        }
        if (id == R.id.nav_user_attendance) {
            Toast.makeText(this, getString(R.string.nav_attendance), Toast.LENGTH_SHORT).show();
            return true;
        }
        if (id == R.id.nav_user_payslips) {
            Toast.makeText(this, getString(R.string.user_dashboard_payslips), Toast.LENGTH_SHORT).show();
            return true;
        }
        if (id == R.id.nav_user_profile) {
            Toast.makeText(this, getString(R.string.nav_profile), Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }
}
