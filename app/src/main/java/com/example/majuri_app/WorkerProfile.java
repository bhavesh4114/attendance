package com.example.majuri_app;

/**
 * Full worker profile model used by WorkerProfileActivity.
 */
public class WorkerProfile {
    private final long id;
    private final String fullName;
    private final String role;
    private final String phone;
    private final String email;
    private final boolean active;
    private final String joinDate;
    private final String address;
    private final String documents;
    private final String profilePhotoUrl;

    public WorkerProfile(
            long id,
            String fullName,
            String role,
            String phone,
            String email,
            boolean active,
            String joinDate,
            String address,
            String documents,
            String profilePhotoUrl
    ) {
        this.id = id;
        this.fullName = fullName;
        this.role = role;
        this.phone = phone;
        this.email = email;
        this.active = active;
        this.joinDate = joinDate;
        this.address = address;
        this.documents = documents;
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public boolean isActive() {
        return active;
    }

    public String getJoinDate() {
        return joinDate;
    }

    public String getAddress() {
        return address;
    }

    public String getDocuments() {
        return documents;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }
}
