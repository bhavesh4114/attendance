package com.example.majuri_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class WorkersListActivity extends AppCompatActivity {

    private WorkersListAdapter adapter;
    private List<WorkerListItem> allItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workers_list);

        allItems = getDummyWorkerListItems();
        adapter = new WorkersListAdapter();
        adapter.setItems(allItems);
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

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> startActivity(new Intent(this, AddWorkerActivity.class)));

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            }
            if (id == R.id.nav_workers) {
                return true;
            }
            if (id == R.id.nav_schedule) {
                Toast.makeText(this, R.string.nav_schedule, Toast.LENGTH_SHORT).show();
                return true;
            }
            if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void filterWorkers(String query) {
        if (query.isEmpty()) {
            adapter.setItems(allItems);
            return;
        }
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

    private List<WorkerListItem> getDummyWorkerListItems() {
        List<WorkerListItem> list = new ArrayList<>();
        list.add(new WorkerListItem("John Doe", "Lead Carpenter", "(555) 0123", true));
        list.add(new WorkerListItem("David Lee", "Mason", "(555) 0124", true));
        list.add(new WorkerListItem("Rajesh Kumar", "Site Supervisor", "+91 98765 43210", true));
        list.add(new WorkerListItem("Amit Singh", "Electrician", "+91 98765 43211", false));
        list.add(new WorkerListItem("Suresh Yadav", "Plumber", "+91 98765 43212", true));
        list.add(new WorkerListItem("Meena Devi", "Helper", "+91 98765 43213", true));
        return list;
    }
}
