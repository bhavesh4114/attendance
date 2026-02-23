package com.example.majuri_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class WorkerManagementActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_management);

        RecyclerView recyclerWorkers = findViewById(R.id.recyclerWorkers);
        recyclerWorkers.setLayoutManager(new LinearLayoutManager(this));
        recyclerWorkers.setNestedScrollingEnabled(false);

        WorkerAdapter adapter = new WorkerAdapter();
        adapter.setWorkers(getDummyWorkers());
        adapter.setOnWorkerClickListener((worker, position) ->
                Toast.makeText(this, getString(R.string.edit) + " " + worker.getName(), Toast.LENGTH_SHORT).show());
        recyclerWorkers.setAdapter(adapter);

        findViewById(R.id.btnMenu).setOnClickListener(v ->
                Toast.makeText(this, R.string.menu, Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnSortBy).setOnClickListener(v ->
                Toast.makeText(this, R.string.sort_by_recent, Toast.LENGTH_SHORT).show());

        MaterialButton btnAddWorker = findViewById(R.id.btnAddWorker);
        btnAddWorker.setOnClickListener(v -> startActivity(new Intent(this, AddWorkerActivity.class)));
    }

    private List<Worker> getDummyWorkers() {
        List<Worker> list = new ArrayList<>();
        list.add(new Worker("Rajesh Kumar", "+91 98765 43210", "₹ 600 / day"));
        list.add(new Worker("Amit Singh", "+91 98765 43211", "₹ 550 / day"));
        list.add(new Worker("Suresh Yadav", "+91 98765 43212", "₹ 650 / day"));
        list.add(new Worker("Meena Devi", "+91 98765 43213", "₹ 500 / day"));
        return list;
    }
}
