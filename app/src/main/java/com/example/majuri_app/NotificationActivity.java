package com.example.majuri_app;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private NotificationAdapter adapter;
    private TextView tvEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        NotificationStore.seedIfEmpty(this);

        RecyclerView recyclerNotifications = findViewById(R.id.recyclerNotifications);
        tvEmptyState = findViewById(R.id.tvNotificationsEmpty);

        adapter = new NotificationAdapter();
        recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));
        recyclerNotifications.setAdapter(adapter);

        findViewById(R.id.btnNotificationBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        NotificationStore.markAllRead(this);
        renderNotifications();
    }

    private void renderNotifications() {
        List<AppNotification> notifications = NotificationStore.getNotifications(this);
        adapter.setItems(notifications);

        boolean isEmpty = notifications.isEmpty();
        tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }
}
