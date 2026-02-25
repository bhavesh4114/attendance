package com.example.majuri_app;

/**
 * Model for a payment queue row.
 */
public class PaymentQueueItem {
    private final long workerId;
    private final String workerName;
    private final String roleAndId;
    private final String dueDate;
    private final double pendingAmountValue;
    private final boolean overdue;

    public PaymentQueueItem(long workerId, String workerName, String roleAndId, String dueDate, double pendingAmountValue, boolean overdue) {
        this.workerId = workerId;
        this.workerName = workerName;
        this.roleAndId = roleAndId;
        this.dueDate = dueDate;
        this.pendingAmountValue = pendingAmountValue;
        this.overdue = overdue;
    }

    public long getWorkerId() { return workerId; }
    public String getWorkerName() { return workerName; }
    public String getRoleAndId() { return roleAndId; }
    public String getDueDate() { return dueDate; }
    public double getPendingAmountValue() { return pendingAmountValue; }
    public boolean isOverdue() { return overdue; }
}
