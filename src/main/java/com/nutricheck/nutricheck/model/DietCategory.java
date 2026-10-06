package com.nutricheck.nutricheck.model;

public enum DietCategory {
    WEIGHT_LOSS("Weight Loss"),
    HEALTHY_LIVING("Healthy Living"),
    WEIGHT_GAIN("Weight Gain");

    private final String displayName;

    DietCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
