package com.example.majuri_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class PaymentsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payments);

        PaymentQueueAdapter adapter = new PaymentQueueAdapter();
        adapter.setItems(getDummyPaymentQueue());
        adapter.setOnMarkPaidClickListener((item, position) ->
                Toast.makeText(this, getString(R.string.mark_as_paid) + " " + item.getWorkerName(), Toast.LENGTH_SHORT).show());

        RecyclerView recyclerPayments = findViewById(R.id.recyclerPayments);
        recyclerPayments.setLayoutManager(new LinearLayoutManager(this));
        recyclerPayments.setAdapter(adapter);
        recyclerPayments.setNestedScrollingEnabled(false);

        findViewById(R.id.btnNotifications).setOnClickListener(v ->
                Toast.makeText(this, R.string.notifications, Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnFilter).setOnClickListener(v ->
                Toast.makeText(this, R.string.filter, Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnMarkAllPaid).setOnClickListener(v ->
                Toast.makeText(this, R.string.mark_all_as_paid, Toast.LENGTH_SHORT).show());
        findViewById(R.id.cardTransactionHistory).setOnClickListener(v ->
                Toast.makeText(this, R.string.transaction_history, Toast.LENGTH_SHORT).show());

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_payments);
        bottomNav.setOnItemSelectedListener(item -> onNavItemSelected(item.getItemId()));
    }

    private List<PaymentQueueItem> getDummyPaymentQueue() {
        List<PaymentQueueItem> list = new ArrayList<>();
        list.add(new PaymentQueueItem("Marco Rossi", "Site Supervisor • ID: 8821", "24 Oct, 2023", "₹2,450.00", false));
        list.add(new PaymentQueueItem("Elena Gilbert", "Electrician • ID: 8822", "Today", "₹1,820.00", false));
        list.add(new PaymentQueueItem("James Anderson", "Mason • ID: 8823", "18 Oct, 2023", "₹5,920.00", true));
        return list;
    }

    private boolean onNavItemSelected(int id) {
        if (id == R.id.nav_dashboard) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return true;
        }
        if (id == R.id.nav_workers) {
            startActivity(new Intent(this, WorkersListActivity.class));
            finish();
            return true;
        }
        if (id == R.id.nav_payments) return true;
        if (id == R.id.nav_analytics) {
            startActivity(new Intent(this, ReportsActivity.class));
            finish();
            return true;
        }
        if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            finish();
            return true;
        }
        return false;
    }
}
