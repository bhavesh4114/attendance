package com.example.majuri_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the Attendance Management staff list (item_attendance_management).
 * Updates summary counts when status changes.
 */
public class AttendanceManagementAdapter extends RecyclerView.Adapter<AttendanceManagementAdapter.StaffViewHolder> {

    private final List<AttendanceStaffItem> items = new ArrayList<>();
    private OnSummaryChangedListener summaryListener;
    private boolean editable = true;

    public interface OnSummaryChangedListener {
        void onSummaryChanged(int present, int halfDay, int absent);
    }

    public void setOnSummaryChangedListener(OnSummaryChangedListener listener) {
        this.summaryListener = listener;
    }

    public void setItems(List<AttendanceStaffItem> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
        notifySummary();
    }

    public List<AttendanceStaffItem> getItems() {
        return new ArrayList<>(items);
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        notifyDataSetChanged();
    }

    public boolean isEditable() {
        return editable;
    }

    private void notifySummary() {
        if (summaryListener == null) return;
        int p = 0, h = 0, a = 0;
        for (AttendanceStaffItem item : items) {
            if (item.getStatus() == AttendanceStaffItem.STATUS_PRESENT) p++;
            else if (item.getStatus() == AttendanceStaffItem.STATUS_HALF_DAY) h++;
            else a++;
        }
        summaryListener.onSummaryChanged(p, h, a);
    }

    @NonNull
    @Override
    public StaffViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance_management, parent, false);
        return new StaffViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StaffViewHolder holder, int position) {
        AttendanceStaffItem item = items.get(position);
        holder.avatarInitials.setText(item.getInitials());
        holder.avatarImage.setVisibility(View.GONE);
        holder.avatarPlaceholder.setVisibility(View.VISIBLE);
        holder.staffName.setText(item.getName());
        holder.staffSubtitle.setText(item.getRole() + " • " + item.getWorkerId());

        holder.optionPresent.setOnClickListener(editable
                ? v -> setStatus(holder.getAdapterPosition(), AttendanceStaffItem.STATUS_PRESENT)
                : null);
        holder.optionHalfDay.setOnClickListener(editable
                ? v -> setStatus(holder.getAdapterPosition(), AttendanceStaffItem.STATUS_HALF_DAY)
                : null);
        holder.optionAbsent.setOnClickListener(editable
                ? v -> setStatus(holder.getAdapterPosition(), AttendanceStaffItem.STATUS_ABSENT)
                : null);

        holder.optionPresent.setClickable(editable);
        holder.optionHalfDay.setClickable(editable);
        holder.optionAbsent.setClickable(editable);
        float alpha = editable ? 1f : 0.8f;
        holder.optionPresent.setAlpha(alpha);
        holder.optionHalfDay.setAlpha(alpha);
        holder.optionAbsent.setAlpha(alpha);

        updateSegmentUi(holder, item.getStatus());
    }

    private void setStatus(int position, int status) {
        if (!editable) return;
        if (position < 0 || position >= items.size()) return;
        items.get(position).setStatus(status);
        notifyItemChanged(position);
        notifySummary();
    }

    private void updateSegmentUi(StaffViewHolder holder, int status) {
        int blueBg = R.drawable.bg_segment_option_selected;
        int transparent = android.R.color.transparent;
        int white = ContextCompat.getColor(holder.itemView.getContext(), R.color.white);
        int grey = ContextCompat.getColor(holder.itemView.getContext(), R.color.medium_grey);

        holder.optionPresent.setBackgroundResource(status == AttendanceStaffItem.STATUS_PRESENT ? blueBg : transparent);
        holder.textPresent.setTextColor(status == AttendanceStaffItem.STATUS_PRESENT ? white : grey);
        holder.textPresent.getPaint().setFakeBoldText(status == AttendanceStaffItem.STATUS_PRESENT);

        holder.optionHalfDay.setBackgroundResource(status == AttendanceStaffItem.STATUS_HALF_DAY ? blueBg : transparent);
        holder.textHalfDay.setTextColor(status == AttendanceStaffItem.STATUS_HALF_DAY ? white : grey);
        holder.textHalfDay.getPaint().setFakeBoldText(status == AttendanceStaffItem.STATUS_HALF_DAY);

        holder.optionAbsent.setBackgroundResource(status == AttendanceStaffItem.STATUS_ABSENT ? blueBg : transparent);
        holder.textAbsent.setTextColor(status == AttendanceStaffItem.STATUS_ABSENT ? white : grey);
        holder.textAbsent.getPaint().setFakeBoldText(status == AttendanceStaffItem.STATUS_ABSENT);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class StaffViewHolder extends RecyclerView.ViewHolder {
        final View avatarPlaceholder;
        final TextView avatarInitials;
        final android.widget.ImageView avatarImage;
        final TextView staffName;
        final TextView staffSubtitle;
        final FrameLayout optionPresent;
        final FrameLayout optionHalfDay;
        final FrameLayout optionAbsent;
        final TextView textPresent;
        final TextView textHalfDay;
        final TextView textAbsent;

        StaffViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarPlaceholder = itemView.findViewById(R.id.avatarPlaceholder);
            avatarInitials = itemView.findViewById(R.id.avatarInitials);
            avatarImage = itemView.findViewById(R.id.avatarImage);
            staffName = itemView.findViewById(R.id.staffName);
            staffSubtitle = itemView.findViewById(R.id.staffSubtitle);
            optionPresent = itemView.findViewById(R.id.optionPresent);
            optionHalfDay = itemView.findViewById(R.id.optionHalfDay);
            optionAbsent = itemView.findViewById(R.id.optionAbsent);
            textPresent = itemView.findViewById(R.id.textPresent);
            textHalfDay = itemView.findViewById(R.id.textHalfDay);
            textAbsent = itemView.findViewById(R.id.textAbsent);
        }
    }
}
