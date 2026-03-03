package com.example.majuri_app;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for the Attendance Management staff list (item_attendance_management).
 * Keeps attendance status persistence behavior unchanged for full-day/half-day/absent.
 * Hourly and Overtime are UI display modes only.
 */
public class AttendanceManagementAdapter extends RecyclerView.Adapter<AttendanceManagementAdapter.StaffViewHolder> {

    private static final int MODE_FULL_DAY = 0;
    private static final int MODE_HALF_DAY = 1;
    private static final int MODE_ABSENT = 2;
    private static final int MODE_HOURLY = 3;
    private static final int MODE_OVERTIME = 4;

    private final List<AttendanceStaffItem> items = new ArrayList<>();
    private final Map<Long, Integer> uiModeByWorkerKey = new HashMap<>();
    private final Map<Long, Boolean> dutyStartedByWorkerKey = new HashMap<>();
    private final Map<Long, Boolean> dutyEndedByWorkerKey = new HashMap<>();
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
        uiModeByWorkerKey.clear();
        dutyStartedByWorkerKey.clear();
        dutyEndedByWorkerKey.clear();
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
        holder.staffSubtitle.setText(item.getRole() + " - " + item.getWorkerId());

        holder.optionPresent.setOnClickListener(editable
                ? v -> setStatusAndMode(holder.getAdapterPosition(), AttendanceStaffItem.STATUS_PRESENT, MODE_FULL_DAY)
                : null);
        holder.optionHalfDay.setOnClickListener(editable
                ? v -> setStatusAndMode(holder.getAdapterPosition(), AttendanceStaffItem.STATUS_HALF_DAY, MODE_HALF_DAY)
                : null);
        holder.optionAbsent.setOnClickListener(editable
                ? v -> setStatusAndMode(holder.getAdapterPosition(), AttendanceStaffItem.STATUS_ABSENT, MODE_ABSENT)
                : null);
        holder.optionHourly.setOnClickListener(editable
                ? v -> setUiModeOnly(holder.getAdapterPosition(), MODE_HOURLY)
                : null);
        holder.optionOvertime.setOnClickListener(editable
                ? v -> setUiModeOnly(holder.getAdapterPosition(), MODE_OVERTIME)
                : null);
        holder.btnInlineSaveAttendance.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition < 0 || adapterPosition >= items.size()) return;
            AttendanceStaffItem clickedItem = items.get(adapterPosition);
            long workerKey = getWorkerKey(clickedItem, adapterPosition);
            boolean started = Boolean.TRUE.equals(dutyStartedByWorkerKey.get(workerKey));
            if (!started) {
                setDutyStarted(adapterPosition, true);
            } else {
                setDutyEnded(adapterPosition, true);
            }
        });
        holder.btnPayNow.setOnClickListener(v -> {
            WorkerDbHelper dbHelper = new WorkerDbHelper(v.getContext());
            double workerDailyWage = dbHelper.getWorkerDailyWageById(item.getWorkerDbId());
            dbHelper.close();
            Intent intent = new Intent(v.getContext(), WorkerPaymentActivity.class);
            intent.putExtra(WorkerPaymentActivity.EXTRA_WORKER_ID, item.getWorkerDbId());
            intent.putExtra(WorkerPaymentActivity.EXTRA_WORKER_NAME, item.getName());
            intent.putExtra(WorkerPaymentActivity.EXTRA_WORKER_ROLE, item.getRole());
            intent.putExtra(WorkerPaymentActivity.EXTRA_WORKER_CODE, item.getWorkerId());
            intent.putExtra(WorkerPaymentActivity.EXTRA_DAILY_WAGE, workerDailyWage);
            intent.putExtra(WorkerPaymentActivity.EXTRA_ATTENDANCE_STATUS, item.getStatus());
            v.getContext().startActivity(intent);
        });
        holder.btnPayLater.setOnClickListener(v ->
                Toast.makeText(v.getContext(), R.string.pay_later, Toast.LENGTH_SHORT).show());

        setOptionEnabledState(holder.optionPresent, editable);
        setOptionEnabledState(holder.optionHalfDay, editable);
        setOptionEnabledState(holder.optionAbsent, editable);
        setOptionEnabledState(holder.optionHourly, editable);
        setOptionEnabledState(holder.optionOvertime, editable);
        setOptionEnabledState(holder.btnPayNow, true);
        setOptionEnabledState(holder.btnPayLater, true);

        int mode = resolveUiMode(item, position);
        updateSegmentUi(holder, item, position, mode);
    }

    private void setOptionEnabledState(View option, boolean enabled) {
        option.setClickable(enabled);
        option.setAlpha(enabled ? 1f : 0.8f);
    }

    private void setStatusAndMode(int position, int status, int mode) {
        if (!editable) return;
        if (position < 0 || position >= items.size()) return;
        AttendanceStaffItem item = items.get(position);
        item.setStatus(status);
        uiModeByWorkerKey.put(getWorkerKey(item, position), mode);
        notifyItemChanged(position);
        notifySummary();
    }

    private void setUiModeOnly(int position, int mode) {
        if (!editable) return;
        if (position < 0 || position >= items.size()) return;
        AttendanceStaffItem item = items.get(position);
        uiModeByWorkerKey.put(getWorkerKey(item, position), mode);
        notifyItemChanged(position);
    }

    private void setDutyEnded(int position, boolean ended) {
        if (position < 0 || position >= items.size()) return;
        AttendanceStaffItem item = items.get(position);
        dutyEndedByWorkerKey.put(getWorkerKey(item, position), ended);
        notifyItemChanged(position);
    }

    private void setDutyStarted(int position, boolean started) {
        if (position < 0 || position >= items.size()) return;
        AttendanceStaffItem item = items.get(position);
        dutyStartedByWorkerKey.put(getWorkerKey(item, position), started);
        notifyItemChanged(position);
    }

    private int resolveUiMode(AttendanceStaffItem item, int position) {
        Integer stored = uiModeByWorkerKey.get(getWorkerKey(item, position));
        if (stored != null) {
            return stored;
        }
        if (item.getStatus() == AttendanceStaffItem.STATUS_HALF_DAY) return MODE_HALF_DAY;
        if (item.getStatus() == AttendanceStaffItem.STATUS_ABSENT) return MODE_ABSENT;
        return MODE_FULL_DAY;
    }

    private long getWorkerKey(AttendanceStaffItem item, int fallbackPosition) {
        long workerId = item != null ? item.getWorkerDbId() : -1L;
        if (workerId > 0L) return workerId;
        String name = item != null ? item.getName() : null;
        if (name != null && !name.trim().isEmpty()) {
            return 1_000_000_000L + name.trim().toLowerCase().hashCode();
        }
        return -1L - fallbackPosition;
    }

    private void updateSegmentUi(StaffViewHolder holder, AttendanceStaffItem item, int position, int mode) {
        int blueBg = R.drawable.bg_segment_option_selected;
        int transparent = android.R.color.transparent;
        int white = ContextCompat.getColor(holder.itemView.getContext(), R.color.white);
        int grey = ContextCompat.getColor(holder.itemView.getContext(), R.color.medium_grey);

        boolean isFull = mode == MODE_FULL_DAY;
        boolean isHalf = mode == MODE_HALF_DAY;
        boolean isAbsent = mode == MODE_ABSENT;
        boolean isHourly = mode == MODE_HOURLY;
        boolean isOvertime = mode == MODE_OVERTIME;

        applyOptionUi(holder.optionPresent, holder.textPresent, isFull, blueBg, transparent, white, grey);
        applyOptionUi(holder.optionHalfDay, holder.textHalfDay, isHalf, blueBg, transparent, white, grey);
        applyOptionUi(holder.optionAbsent, holder.textAbsent, isAbsent, blueBg, transparent, white, grey);
        applyOptionUi(holder.optionHourly, holder.textHourly, isHourly, blueBg, transparent, white, grey);
        applyOptionUi(holder.optionOvertime, holder.textOvertime, isOvertime, blueBg, transparent, white, grey);

        holder.hourlyContainer.setVisibility(isHourly ? View.VISIBLE : View.GONE);
        holder.overtimeContainer.setVisibility(isOvertime ? View.VISIBLE : View.GONE);
        boolean showDutyActions = isFull || isHalf;
        boolean dutyStarted = Boolean.TRUE.equals(dutyStartedByWorkerKey.get(getWorkerKey(item, position)));
        boolean dutyEnded = Boolean.TRUE.equals(dutyEndedByWorkerKey.get(getWorkerKey(item, position)));

        holder.btnInlineSaveAttendance.setVisibility(showDutyActions && !dutyEnded ? View.VISIBLE : View.GONE);
        holder.paymentActionContainer.setVisibility(showDutyActions && dutyEnded ? View.VISIBLE : View.GONE);
        holder.btnInlineSaveAttendance.setText(dutyStarted ? R.string.end_duty : R.string.start_duty);
        holder.btnInlineSaveAttendance.setEnabled(true);
        holder.paymentActionContainer.setEnabled(true);
        holder.btnInlineSaveAttendance.setAlpha(1f);
        holder.paymentActionContainer.setAlpha(1f);
    }

    private void applyOptionUi(
            FrameLayout optionView,
            TextView textView,
            boolean selected,
            int selectedBg,
            int unselectedBg,
            int selectedColor,
            int unselectedColor
    ) {
        optionView.setBackgroundResource(selected ? selectedBg : unselectedBg);
        textView.setTextColor(selected ? selectedColor : unselectedColor);
        textView.getPaint().setFakeBoldText(selected);
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
        final FrameLayout optionHourly;
        final FrameLayout optionOvertime;
        final TextView textPresent;
        final TextView textHalfDay;
        final TextView textAbsent;
        final TextView textHourly;
        final TextView textOvertime;
        final View hourlyContainer;
        final View overtimeContainer;
        final MaterialButton btnInlineSaveAttendance;
        final View paymentActionContainer;
        final View btnPayNow;
        final View btnPayLater;

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
            optionHourly = itemView.findViewById(R.id.optionHourly);
            optionOvertime = itemView.findViewById(R.id.optionOvertime);
            textPresent = itemView.findViewById(R.id.textPresent);
            textHalfDay = itemView.findViewById(R.id.textHalfDay);
            textAbsent = itemView.findViewById(R.id.textAbsent);
            textHourly = itemView.findViewById(R.id.textHourly);
            textOvertime = itemView.findViewById(R.id.textOvertime);
            hourlyContainer = itemView.findViewById(R.id.hourlyContainer);
            overtimeContainer = itemView.findViewById(R.id.overtimeContainer);
            btnInlineSaveAttendance = itemView.findViewById(R.id.btnInlineSaveAttendance);
            paymentActionContainer = itemView.findViewById(R.id.paymentActionContainer);
            btnPayNow = itemView.findViewById(R.id.btnPayNow);
            btnPayLater = itemView.findViewById(R.id.btnPayLater);
        }
    }
}
