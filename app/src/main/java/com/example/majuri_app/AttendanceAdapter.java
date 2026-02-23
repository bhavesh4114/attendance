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

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.CardViewHolder> {

    private final List<AttendanceCard> items = new ArrayList<>();

    public void setItems(List<AttendanceCard> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance_card, parent, false);
        return new CardViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder h, int position) {
        AttendanceCard card = items.get(position);
        h.workerName.setText(card.getName());
        h.workerRole.setText(card.getRole());
        h.tvOtHours.setText(String.valueOf(card.getOtHours()));

        AttendanceCard.Status s = card.getStatus();
        setPresentPill(h.btnPresent, h.iconPresent, h.textPresent, s == AttendanceCard.Status.PRESENT);
        setAbsentPill(h.btnAbsent, h.iconAbsent, h.textAbsent, s == AttendanceCard.Status.ABSENT);
        setHalfDayPill(h.btnHalfDay, h.iconHalfDay, h.textHalfDay, s == AttendanceCard.Status.HALF_DAY);
    }

    private void setPresentPill(View btn, ImageView icon, TextView text, boolean selected) {
        btn.setBackgroundResource(selected ? R.drawable.bg_pill_present : R.drawable.bg_pill_unselected);
        icon.setImageResource(selected ? R.drawable.ic_check_green : R.drawable.ic_check_grey);
        text.setTextColor(btn.getContext().getResources().getColor(selected ? R.color.green_text : R.color.medium_grey, null));
    }

    private void setAbsentPill(View btn, ImageView icon, TextView text, boolean selected) {
        btn.setBackgroundResource(selected ? R.drawable.bg_pill_absent : R.drawable.bg_pill_unselected);
        icon.setImageResource(selected ? R.drawable.ic_close_red : R.drawable.ic_close_grey);
        text.setTextColor(btn.getContext().getResources().getColor(selected ? R.color.red_status : R.color.medium_grey, null));
    }

    private void setHalfDayPill(View btn, ImageView icon, TextView text, boolean selected) {
        btn.setBackgroundResource(selected ? R.drawable.bg_pill_halfday : R.drawable.bg_pill_unselected);
        icon.setImageResource(selected ? R.drawable.ic_halfday : R.drawable.ic_halfday_grey);
        text.setTextColor(btn.getContext().getResources().getColor(selected ? R.color.orange_status : R.color.medium_grey, null));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        final TextView workerName, workerRole, tvOtHours;
        final View btnPresent, btnAbsent, btnHalfDay;
        final ImageView iconPresent, iconAbsent, iconHalfDay;
        final TextView textPresent, textAbsent, textHalfDay;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            workerName = itemView.findViewById(R.id.workerName);
            workerRole = itemView.findViewById(R.id.workerRole);
            tvOtHours = itemView.findViewById(R.id.tvOtHours);
            btnPresent = itemView.findViewById(R.id.btnPresent);
            btnAbsent = itemView.findViewById(R.id.btnAbsent);
            btnHalfDay = itemView.findViewById(R.id.btnHalfDay);
            iconPresent = itemView.findViewById(R.id.iconPresent);
            iconAbsent = itemView.findViewById(R.id.iconAbsent);
            iconHalfDay = itemView.findViewById(R.id.iconHalfDay);
            textPresent = itemView.findViewById(R.id.textPresent);
            textAbsent = itemView.findViewById(R.id.textAbsent);
            textHalfDay = itemView.findViewById(R.id.textHalfDay);
        }
    }
}
