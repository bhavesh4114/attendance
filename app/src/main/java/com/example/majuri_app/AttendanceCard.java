package com.example.majuri_app;

/**
 * Dummy model for daily attendance card (design only).
 */
public class AttendanceCard {
    public enum Status { PRESENT, ABSENT, HALF_DAY }

    private final String name;
    private final String role;
    private final int otHours;
    private final Status status;

    public AttendanceCard(String name, String role, int otHours, Status status) {
        this.name = name;
        this.role = role;
        this.otHours = otHours;
        this.status = status;
    }

    public String getName() { return name; }
    public String getRole() { return role; }
    public int getOtHours() { return otHours; }
    public Status getStatus() { return status; }
}
