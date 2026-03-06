package com.example.majuri_app;

/**
 * Monthly payment summary derived from worker profile, attendance and paid advances.
 */
public class WorkerPaymentSummary {
    private final long workerId;
    private final String workerName;
    private final String role;
    private final double dailyWage;
    private final double workedDays;
    private final double overtimeHours;
    private final double grossAmount;
    private final double paidAmount;
    private final double pendingAmount;

    public WorkerPaymentSummary(
            long workerId,
            String workerName,
            String role,
            double dailyWage,
            double workedDays,
            double overtimeHours,
            double grossAmount,
            double paidAmount,
            double pendingAmount
    ) {
        this.workerId = workerId;
        this.workerName = workerName;
        this.role = role;
        this.dailyWage = dailyWage;
        this.workedDays = workedDays;
        this.overtimeHours = overtimeHours;
        this.grossAmount = grossAmount;
        this.paidAmount = paidAmount;
        this.pendingAmount = pendingAmount;
    }

    public long getWorkerId() {
        return workerId;
    }

    public String getWorkerName() {
        return workerName;
    }

    public String getRole() {
        return role;
    }

    public double getDailyWage() {
        return dailyWage;
    }

    public double getWorkedDays() {
        return workedDays;
    }

    public double getOvertimeHours() {
        return overtimeHours;
    }

    public double getGrossAmount() {
        return grossAmount;
    }

    public double getPaidAmount() {
        return paidAmount;
    }

    public double getPendingAmount() {
        return pendingAmount;
    }
}
