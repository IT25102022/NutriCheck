package com.nutricheck.nutricheck.model;

public enum BmiCategory {
    UNDERWEIGHT("Underweight", "Below 18.5", "badge-blue", "Focus on nutrient-dense meals, healthy fats, and calorie surplus for gradual weight gain."),
    NORMAL("Normal Weight", "18.5 - 24.9", "badge-green", "Maintain a balanced Mediterranean diet with regular physical activity and optimal hydration."),
    OVERWEIGHT("Overweight", "25.0 - 29.9", "badge-amber", "Aim for a moderate calorie deficit, high-fiber whole foods, and consistent aerobic exercise."),
    OBESE("Obese", "30.0 and above", "badge-rose", "Adopt a structured low-glycemic, portion-controlled nutritional plan with professional supervision.");

    private final String displayName;
    private final String rangeText;
    private final String badgeClass;
    private final String recommendationSummary;

    BmiCategory(String displayName, String rangeText, String badgeClass, String recommendationSummary) {
        this.displayName = displayName;
        this.rangeText = rangeText;
        this.badgeClass = badgeClass;
        this.recommendationSummary = recommendationSummary;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRangeText() {
        return rangeText;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public String getRecommendationSummary() {
        return recommendationSummary;
    }

    public static BmiCategory fromBmi(double bmi) {
        if (bmi < 18.5) {
            return UNDERWEIGHT;
        } else if (bmi <= 24.9) {
            return NORMAL;
        } else if (bmi <= 29.9) {
            return OVERWEIGHT;
        } else {
            return OBESE;
        }
    }
}
