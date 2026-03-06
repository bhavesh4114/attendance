package com.example.majuri_app;

/**
 * Model for a row on the Workers List screen (name, role, phone, active status).
 */
public class WorkerListItem {
    private final long id;
    private final String name;
    private final String role;
    private final String phone;
    private final boolean active;
    private final double dailyWage;

    public WorkerListItem(long id, String name, String role, String phone, boolean active) {
        this(id, name, role, phone, active, 0d);
    }

    public WorkerListItem(long id, String name, String role, String phone, boolean active, double dailyWage) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.phone = phone;
        this.active = active;
        this.dailyWage = Math.max(0d, dailyWage);
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isActive() {
        return active;
    }

    public double getDailyWage() {
        return dailyWage;
    }
}
