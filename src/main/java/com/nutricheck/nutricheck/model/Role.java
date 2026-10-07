package com.nutricheck.nutricheck.model;

public enum Role {
    ROLE_PATIENT("Patient"),
    ROLE_NUTRITIONIST("Nutritionist"),
    ROLE_HEALTHCARE_PROFESSIONAL("Healthcare Professional"),
    ROLE_ADMIN("System Administrator");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
