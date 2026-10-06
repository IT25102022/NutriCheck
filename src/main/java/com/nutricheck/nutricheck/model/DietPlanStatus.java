package com.nutricheck.nutricheck.model;

public enum DietPlanStatus {
    PENDING_APPROVAL("Pending Approval", "badge-amber"),
    APPROVED("Approved", "badge-green"),
    REJECTED("Rejected", "badge-rose");

    private final String displayName;
    private final String badgeClass;

    DietPlanStatus(String displayName, String badgeClass) {
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
