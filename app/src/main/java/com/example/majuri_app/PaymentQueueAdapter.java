package com.example.majuri_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PaymentQueueAdapter extends RecyclerView.Adapter<PaymentQueueAdapter.PaymentViewHolder> {

    private final List<PaymentQueueItem> items = new ArrayList<>();
    private OnMarkPaidClickListener markPaidListener;

    public interface OnMarkPaidClickListener {
        void onMarkPaidClick(PaymentQueueItem item, int position);
    }

    public void setOnMarkPaidClickListener(OnMarkPaidClickListener listener) {
        this.markPaidListener = listener;
    }

    public void setItems(List<PaymentQueueItem> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PaymentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment_card, parent, false);
        return new PaymentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaymentViewHolder holder, int position) {
        PaymentQueueItem item = items.get(position);
        holder.workerName.setText(item.getWorkerName());
        holder.workerRoleId.setText(item.getRoleAndId());
        holder.dueDate.setText(item.getDueDate());
        holder.pendingAmount.setText(formatCurrency(item.getPendingAmountValue()));

        if (item.isOverdue()) {
            holder.cardRoot.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.overdue_light_red_bg));
            holder.cardRoot.setStrokeWidth((int) (1 * holder.itemView.getContext().getResources().getDisplayMetrics().density));
            holder.cardRoot.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.overdue_border));
            holder.overdueLabel.setVisibility(View.VISIBLE);
            holder.pendingAmount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.red_dark));
        } else {
            holder.cardRoot.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.card_white));
            holder.cardRoot.setStrokeWidth(0);
            holder.overdueLabel.setVisibility(View.GONE);
            holder.pendingAmount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.red_status));
        }

        holder.workerAvatar.setImageDrawable(null);
        holder.workerAvatar.setBackgroundResource(R.drawable.bg_avatar_placeholder);

        holder.btnMarkPaid.setOnClickListener(v -> {
            if (markPaidListener != null) {
                markPaidListener.onMarkPaidClick(item, holder.getAdapterPosition());
            }
        });
        holder.btnMarkPaid.setEnabled(item.getPendingAmountValue() > 0d);
        holder.btnMarkPaid.setAlpha(item.getPendingAmountValue() > 0d ? 1f : 0.6f);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        formatter.setMaximumFractionDigits(2);
        formatter.setMinimumFractionDigits(0);
        return formatter.format(Math.max(0d, amount));
    }

    static class PaymentViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardRoot;
        final ImageView workerAvatar;
        final TextView workerName;
        final TextView workerRoleId;
        final View overdueLabel;
        final TextView dueDate;
        final TextView pendingAmount;
        final MaterialButton btnMarkPaid;

        PaymentViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardRoot);
            workerAvatar = itemView.findViewById(R.id.workerAvatar);
            workerName = itemView.findViewById(R.id.workerName);
            workerRoleId = itemView.findViewById(R.id.workerRoleId);
            overdueLabel = itemView.findViewById(R.id.overdueLabel);
            dueDate = itemView.findViewById(R.id.dueDate);
            pendingAmount = itemView.findViewById(R.id.pendingAmount);
            btnMarkPaid = itemView.findViewById(R.id.btnMarkPaid);
        }
    }
}
