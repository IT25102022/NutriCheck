package com.nutricheck.nutricheck.model;

public enum FeedbackStatus {
    NEW("New", "badge-amber"),
    REVIEWED("Reviewed", "badge-blue"),
    RESOLVED("Resolved", "badge-green");

    private final String displayName;
    private final String badgeClass;

    FeedbackStatus(String displayName, String badgeClass) {
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
