package com.example.majuri_app;

/**
 * Model for a worker in the Worker Management list.
 */
public class Worker {
    private final String name;
    private final String phone;
    private final String salaryPerDay;
    private final String imageUrl; // optional; null for placeholder

    public Worker(String name, String phone, String salaryPerDay) {
        this(name, phone, salaryPerDay, null);
    }

    public Worker(String name, String phone, String salaryPerDay, String imageUrl) {
        this.name = name;
        this.phone = phone;
        this.salaryPerDay = salaryPerDay;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getSalaryPerDay() {
        return salaryPerDay;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
