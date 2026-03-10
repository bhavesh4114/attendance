package com.example.majuri_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the premium Workers List screen (item_worker_list).
 */
public class WorkersListAdapter extends RecyclerView.Adapter<WorkersListAdapter.WorkerListViewHolder> {

    private final List<WorkerListItem> items = new ArrayList<>();
    private OnViewClickListener viewClickListener;

    public interface OnViewClickListener {
        void onViewClick(WorkerListItem item, int position);
    }

    public void setOnViewClickListener(OnViewClickListener listener) {
        this.viewClickListener = listener;
    }

    public void setItems(List<WorkerListItem> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WorkerListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_worker_list, parent, false);
        return new WorkerListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkerListViewHolder holder, int position) {
        WorkerListItem item = items.get(position);
        holder.workerName.setText(item.getName());
        holder.workerRole.setText(item.getRole());
        holder.workerPhone.setText(item.getPhone());

        if (item.isActive()) {
            holder.statusChip.setBackgroundResource(R.drawable.bg_chip_active);
            holder.statusChip.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.active_green));
            holder.statusChip.setText(R.string.active);
            if (holder.statusDot != null) holder.statusDot.setVisibility(View.VISIBLE);
        } else {
            holder.statusChip.setBackgroundResource(R.drawable.bg_chip_inactive);
            holder.statusChip.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.medium_grey));
            holder.statusChip.setText(R.string.inactive);
            if (holder.statusDot != null) holder.statusDot.setVisibility(View.INVISIBLE);
        }

        holder.workerAvatar.setImageDrawable(null);
        holder.workerAvatar.setBackgroundResource(R.drawable.bg_avatar_placeholder);

        holder.btnView.setOnClickListener(v -> {
            if (viewClickListener != null) {
                viewClickListener.onViewClick(item, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class WorkerListViewHolder extends RecyclerView.ViewHolder {
        final ImageView workerAvatar;
        final View statusDot;
        final TextView workerName;
        final TextView statusChip;
        final TextView workerRole;
        final TextView workerPhone;
        final MaterialButton btnView;

        WorkerListViewHolder(@NonNull View itemView) {
            super(itemView);
            workerAvatar = itemView.findViewById(R.id.workerAvatar);
            statusDot = itemView.findViewById(R.id.statusDot);
            workerName = itemView.findViewById(R.id.workerName);
            statusChip = itemView.findViewById(R.id.statusChip);
            workerRole = itemView.findViewById(R.id.workerRole);
            workerPhone = itemView.findViewById(R.id.workerPhone);
            btnView = itemView.findViewById(R.id.btnView);
        }
    }
}
