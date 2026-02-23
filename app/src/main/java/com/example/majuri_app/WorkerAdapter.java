package com.example.majuri_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class WorkerAdapter extends RecyclerView.Adapter<WorkerAdapter.WorkerViewHolder> {

    private final List<Worker> workers = new ArrayList<>();
    private OnWorkerClickListener listener;

    public interface OnWorkerClickListener {
        void onEditClick(Worker worker, int position);
    }

    public void setOnWorkerClickListener(OnWorkerClickListener listener) {
        this.listener = listener;
    }

    public void setWorkers(List<Worker> list) {
        workers.clear();
        if (list != null) {
            workers.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WorkerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_worker, parent, false);
        return new WorkerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkerViewHolder holder, int position) {
        Worker worker = workers.get(position);
        holder.workerName.setText(worker.getName());
        holder.workerPhone.setText(worker.getPhone());
        holder.workerSalary.setText(worker.getSalaryPerDay());
        // Avatar: use placeholder background (no image URL loading for dummy data)
        holder.workerAvatar.setImageDrawable(null);
        holder.workerAvatar.setBackgroundResource(R.drawable.bg_avatar_placeholder);

        holder.btnEditWorker.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(worker, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return workers.size();
    }

    static class WorkerViewHolder extends RecyclerView.ViewHolder {
        final ImageView workerAvatar;
        final TextView workerName;
        final TextView workerPhone;
        final TextView workerSalary;
        final View btnEditWorker;

        WorkerViewHolder(@NonNull View itemView) {
            super(itemView);
            workerAvatar = itemView.findViewById(R.id.workerAvatar);
            workerName = itemView.findViewById(R.id.workerName);
            workerPhone = itemView.findViewById(R.id.workerPhone);
            workerSalary = itemView.findViewById(R.id.workerSalary);
            btnEditWorker = itemView.findViewById(R.id.btnEditWorker);
        }
    }
}
