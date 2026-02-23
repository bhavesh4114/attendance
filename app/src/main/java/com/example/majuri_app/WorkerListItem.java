package com.example.majuri_app;

/**
 * Model for a row on the Workers List screen (name, role, phone, active status).
 */
public class WorkerListItem {
    private final String name;
    private final String role;
    private final String phone;
    private final boolean active;

    public WorkerListItem(String name, String role, String phone, boolean active) {
        this.name = name;
        this.role = role;
        this.phone = phone;
        this.active = active;
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
}
