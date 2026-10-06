package com.nutricheck.nutricheck.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_records")
public class HealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "Height is required")
    @DecimalMin(value = "50.0", message = "Height must be at least 50 cm")
    @DecimalMax(value = "280.0", message = "Height cannot exceed 280 cm")
    @Column(nullable = false)
    private Double heightCm;

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "20.0", message = "Weight must be at least 20 kg")
    @DecimalMax(value = "400.0", message = "Weight cannot exceed 400 kg")
    @Column(nullable = false)
    private Double weightKg;

    // Blood pressure (e.g. 120/80)
    private Integer systolicBp;
    private Integer diastolicBp;

    // Blood sugar (mg/dL)
    private Double bloodSugar;

    // Total Cholesterol (mg/dL)
    private Double cholesterol;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private LocalDateTime recordedAt = LocalDateTime.now();

    public HealthRecord() {
    }

    public HealthRecord(User user, Double heightCm, Double weightKg, Integer systolicBp, Integer diastolicBp, Double bloodSugar, Double cholesterol, String notes) {
        this.user = user;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.systolicBp = systolicBp;
        this.diastolicBp = diastolicBp;
        this.bloodSugar = bloodSugar;
        this.cholesterol = cholesterol;
        this.notes = notes;
        this.recordedAt = LocalDateTime.now();
    }

    // Calculated helper methods
    public Double getCalculatedBmi() {
        if (heightCm != null && weightKg != null && heightCm > 0) {
            double heightM = heightCm / 100.0;
            return Math.round((weightKg / (heightM * heightM)) * 100.0) / 100.0;
        }
        return null;
    }

    public BmiCategory getBmiCategory() {
        Double bmi = getCalculatedBmi();
        return bmi != null ? BmiCategory.fromBmi(bmi) : null;
    }

    public String getBloodPressureText() {
        if (systolicBp != null && diastolicBp != null) {
            return systolicBp + "/" + diastolicBp + " mmHg";
        }
        return "N/A";
    }

    // Getters and Setters
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

    public Integer getSystolicBp() {
        return systolicBp;
    }

    public void setSystolicBp(Integer systolicBp) {
        this.systolicBp = systolicBp;
    }

    public Integer getDiastolicBp() {
        return diastolicBp;
    }

    public void setDiastolicBp(Integer diastolicBp) {
        this.diastolicBp = diastolicBp;
    }

    public Double getBloodSugar() {
        return bloodSugar;
    }

    public void setBloodSugar(Double bloodSugar) {
        this.bloodSugar = bloodSugar;
    }

    public Double getCholesterol() {
        return cholesterol;
    }

    public void setCholesterol(Double cholesterol) {
        this.cholesterol = cholesterol;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}
