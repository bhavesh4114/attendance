package com.example.majuri_app;

/**
 * Model for a payment queue row.
 */
public class PaymentQueueItem {
    private final String workerName;
    private final String roleAndId;
    private final String dueDate;
    private final String pendingAmount;
    private final boolean overdue;

    public PaymentQueueItem(String workerName, String roleAndId, String dueDate, String pendingAmount, boolean overdue) {
        this.workerName = workerName;
        this.roleAndId = roleAndId;
        this.dueDate = dueDate;
        this.pendingAmount = pendingAmount;
        this.overdue = overdue;
    }

    public String getWorkerName() { return workerName; }
    public String getRoleAndId() { return roleAndId; }
    public String getDueDate() { return dueDate; }
    public String getPendingAmount() { return pendingAmount; }
    public boolean isOverdue() { return overdue; }
}
