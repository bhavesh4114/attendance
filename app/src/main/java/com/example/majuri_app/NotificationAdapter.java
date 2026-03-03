package com.example.majuri_app;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<AppNotification> items = new ArrayList<>();

    public void setItems(List<AppNotification> notifications) {
        items.clear();
        if (notifications != null) {
            items.addAll(notifications);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        AppNotification item = items.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvMessage.setText(item.getMessage());
        holder.tvTime.setText(item.getTimeLabel());

        boolean unread = item.isUnread();
        holder.unreadDot.setVisibility(unread ? View.VISIBLE : View.GONE);
        holder.tvTitle.setTypeface(Typeface.DEFAULT, unread ? Typeface.BOLD : Typeface.NORMAL);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvMessage;
        final TextView tvTime;
        final View unreadDot;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            unreadDot = itemView.findViewById(R.id.notificationUnreadDot);
        }
    }
}
