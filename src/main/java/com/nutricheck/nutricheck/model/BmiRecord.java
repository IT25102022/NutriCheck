package com.nutricheck.nutricheck.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bmi_records")
public class BmiRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Double heightCm;

    @Column(nullable = false)
    private Double weightKg;

    @Column(nullable = false)
    private Double bmiValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BmiCategory bmiCategory;

    @Column
    private Integer age;

    @Column(nullable = false)
    private LocalDateTime recordedAt = LocalDateTime.now();

    public BmiRecord() {
    }

    public BmiRecord(User user, Double heightCm, Double weightKg, Double bmiValue, BmiCategory bmiCategory) {
        this(user, heightCm, weightKg, bmiValue, bmiCategory, user != null ? user.getAge() : null);
    }

    public BmiRecord(User user, Double heightCm, Double weightKg, Double bmiValue, BmiCategory bmiCategory, Integer age) {
        this.user = user;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.bmiValue = bmiValue;
        this.bmiCategory = bmiCategory;
        this.age = age;
        this.recordedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Double getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(Double heightCm) {
        this.heightCm = heightCm;
    }

    public Double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(Double weightKg) {
        this.weightKg = weightKg;
    }

    public Double getBmiValue() {
        return bmiValue;
    }

    public void setBmiValue(Double bmiValue) {
        this.bmiValue = bmiValue;
    }

    public BmiCategory getBmiCategory() {
        return bmiCategory;
    }

    public void setBmiCategory(BmiCategory bmiCategory) {
        this.bmiCategory = bmiCategory;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}
