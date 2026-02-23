package com.example.majuri_app;

/**
 * Model for a staff row on the Attendance Management screen.
 * status: 0 = Present, 1 = Half Day, 2 = Absent
 */
public class AttendanceStaffItem {
    public static final int STATUS_PRESENT = 0;
    public static final int STATUS_HALF_DAY = 1;
    public static final int STATUS_ABSENT = 2;

    private final String name;
    private final String role;
    private final String workerId;
    private final String initials;
    private int status;

    public AttendanceStaffItem(String name, String role, String workerId, int status) {
        this.name = name;
        this.role = role;
        this.workerId = workerId;
        this.status = status;
        this.initials = getInitials(name);
    }

    private static String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
        }
        return parts[0].length() >= 2 ? parts[0].substring(0, 2).toUpperCase() : parts[0].toUpperCase();
    }

    public String getName() { return name; }
    public String getRole() { return role; }
    public String getWorkerId() { return workerId; }
    public String getInitials() { return initials; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
}
