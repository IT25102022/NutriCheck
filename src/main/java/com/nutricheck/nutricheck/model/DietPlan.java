package com.nutricheck.nutricheck.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "diet_plans")
public class DietPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(nullable = false, length = 150)
    private String title;

    @NotNull(message = "Diet category is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DietCategory category;

    @NotNull(message = "Target BMI category is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BmiCategory targetBmiCategory;

    @NotBlank(message = "Description is required")
    @Column(nullable = false, length = 500)
    private String shortDescription;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String mealPlanDetails; // Breakdown for Breakfast, Lunch, Dinner, Snacks, Guidelines

    private Integer dailyCalories;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DietPlanStatus status = DietPlanStatus.PENDING_APPROVAL;

    @Column(length = 500)
    private String reviewNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public DietPlan() {
    }

    public DietPlan(String title, DietCategory category, BmiCategory targetBmiCategory, String shortDescription, String mealPlanDetails, Integer dailyCalories, User createdBy) {
        this.title = title;
        this.category = category;
        this.targetBmiCategory = targetBmiCategory;
        this.shortDescription = shortDescription;
        this.mealPlanDetails = mealPlanDetails;
        this.dailyCalories = dailyCalories;
        this.createdBy = createdBy;
        this.status = DietPlanStatus.PENDING_APPROVAL;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DietCategory getCategory() {
        return category;
    }

    public void setCategory(DietCategory category) {
        this.category = category;
    }

    public BmiCategory getTargetBmiCategory() {
        return targetBmiCategory;
    }

    public void setTargetBmiCategory(BmiCategory targetBmiCategory) {
        this.targetBmiCategory = targetBmiCategory;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getMealPlanDetails() {
        return mealPlanDetails;
    }

    public void setMealPlanDetails(String mealPlanDetails) {
        this.mealPlanDetails = mealPlanDetails;
    }

    public Integer getDailyCalories() {
        return dailyCalories;
    }

    public void setDailyCalories(Integer dailyCalories) {
        this.dailyCalories = dailyCalories;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public DietPlanStatus getStatus() {
        return status;
    }

    public void setStatus(DietPlanStatus status) {
        this.status = status;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
