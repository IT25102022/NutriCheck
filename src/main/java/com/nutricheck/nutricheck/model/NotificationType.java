package com.nutricheck.nutricheck.model;

public enum NotificationType {
    SYSTEM("System", "badge-blue"),
    APPOINTMENT("Appointment", "badge-green"),
    CLINICAL("Clinical", "badge-amber"),
    ANNOUNCEMENT("Announcement", "badge-rose");

    private final String displayName;
    private final String badgeClass;

    NotificationType(String displayName, String badgeClass) {
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
