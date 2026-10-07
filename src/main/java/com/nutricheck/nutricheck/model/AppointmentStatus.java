package com.nutricheck.nutricheck.model;

public enum AppointmentStatus {
    PENDING("Pending", "badge-amber"),
    CONFIRMED("Confirmed", "badge-green"),
    COMPLETED("Completed", "badge-blue"),
    CANCELLED("Cancelled", "badge-rose");

    private final String displayName;
    private final String badgeClass;

    AppointmentStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
