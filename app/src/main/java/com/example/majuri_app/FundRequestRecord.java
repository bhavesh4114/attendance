package com.example.majuri_app;

public class FundRequestRecord {
    private final long id;
    private final String contractorId;
    private final String companyName;
    private final double amount;
    private final String note;
    private final String status;
    private final String createdAt;

    public FundRequestRecord(
            long id,
            String contractorId,
            String companyName,
            double amount,
            String note,
            String status,
            String createdAt
    ) {
        this.id = id;
        this.contractorId = contractorId != null ? contractorId : "";
        this.companyName = companyName != null ? companyName : "";
        this.amount = amount;
        this.note = note != null ? note : "";
        this.status = status != null ? status : "Pending";
        this.createdAt = createdAt != null ? createdAt : "";
    }

    public long getId() {
        return id;
    }

    public String getContractorId() {
        return contractorId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public double getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
