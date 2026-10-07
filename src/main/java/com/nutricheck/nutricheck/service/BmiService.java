package com.nutricheck.nutricheck.service;

import com.nutricheck.nutricheck.model.BmiCategory;
import com.nutricheck.nutricheck.model.BmiRecord;
import com.nutricheck.nutricheck.model.User;
import com.nutricheck.nutricheck.repository.BmiRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class BmiService {

    private final BmiRecordRepository bmiRecordRepository;

    public BmiService(BmiRecordRepository bmiRecordRepository) {
        this.bmiRecordRepository = bmiRecordRepository;
    }

    /**
     * Calculate BMI value using formula: Weight (kg) / (Height (m))^2
     */
    public double calculateBmi(double weightKg, double heightCm) {
        return calculateBmi(weightKg, heightCm, null);
    }

    /**
     * Calculate BMI value using formula: Weight (kg) / (Height (m))^2
     * Age is validated and made available for interpretation without altering the adult mathematical formula.
     */
    public double calculateBmi(double weightKg, double heightCm, Integer age) {
        if (heightCm <= 0 || weightKg <= 0) {
            throw new IllegalArgumentException("Height and weight must be greater than zero.");
        }
        if (heightCm < 50.0 || heightCm > 280.0) {
            throw new IllegalArgumentException("Height must be between 50 and 280 cm.");
        }
        if (weightKg < 20.0 || weightKg > 400.0) {
            throw new IllegalArgumentException("Weight must be between 20 and 400 kg.");
        }
        if (age != null && (age < 1 || age > 120)) {
            throw new IllegalArgumentException("Age must be between 1 and 120 years.");
        }
        double heightM = heightCm / 100.0;
        double rawBmi = weightKg / (heightM * heightM);
        return Math.round(rawBmi * 100.0) / 100.0; // 2 decimal precision
    }

    /**
     * Classify BMI category using standard WHO guidelines
     */
    public BmiCategory classifyBmi(double bmiValue) {
        return BmiCategory.fromBmi(bmiValue);
    }

    /**
     * Classify BMI category with age context
     */
    public BmiCategory classifyBmi(double bmiValue, Integer age) {
        return BmiCategory.fromBmi(bmiValue);
    }

    /**
     * Save a BMI measurement record for a user
     */
    @Transactional
    public BmiRecord recordBmi(User user, double heightCm, double weightKg) {
        Integer age = (user != null) ? user.getAge() : null;
        return recordBmi(user, heightCm, weightKg, age);
    }

    /**
     * Save a BMI measurement record with specific age for a user
     */
    @Transactional
    public BmiRecord recordBmi(User user, double heightCm, double weightKg, Integer age) {
        if (age == null && user != null) {
            age = user.getAge();
        }
        double bmiValue = calculateBmi(weightKg, heightCm, age);
        BmiCategory category = classifyBmi(bmiValue, age);

        BmiRecord record = new BmiRecord(user, heightCm, weightKg, bmiValue, category, age);
        return bmiRecordRepository.save(record);
    }

    public List<BmiRecord> getUserBmiHistory(User user) {
        return bmiRecordRepository.findByUserOrderByRecordedAtAsc(user);
    }

    public Optional<BmiRecord> getLatestBmiRecord(User user) {
        return bmiRecordRepository.findFirstByUserOrderByRecordedAtDesc(user);
    }

    /**
     * Update an existing BMI measurement for the owning user.
     * BMI and category are always recalculated from the supplied height and weight.
     */
    @Transactional
    public BmiRecord updateBmiRecord(Long recordId, double heightCm, double weightKg, User user) {
        return updateBmiRecord(recordId, heightCm, weightKg, null, user);
    }

    /**
     * Update an existing BMI measurement with age for the owning user.
     */
    @Transactional
    public BmiRecord updateBmiRecord(Long recordId, double heightCm, double weightKg, Integer age, User user) {
        BmiRecord record = bmiRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("BMI record not found."));

        if (!record.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to update this BMI record.");
        }

        if (age == null) {
            age = record.getAge() != null ? record.getAge() : user.getAge();
        }

        double bmiValue = calculateBmi(weightKg, heightCm, age);
        BmiCategory category = classifyBmi(bmiValue, age);

        record.setHeightCm(heightCm);
        record.setWeightKg(weightKg);
        record.setAge(age);
        record.setBmiValue(bmiValue);
        record.setBmiCategory(category);

        return bmiRecordRepository.save(record);
    }

    public Map<String, Long> getBmiDistributionStats() {
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (BmiCategory cat : BmiCategory.values()) {
            distribution.put(cat.getDisplayName(), bmiRecordRepository.countByBmiCategory(cat));
        }
        return distribution;
    }

    @Transactional
    public void deleteBmiRecord(Long recordId, User user) {
        BmiRecord record = bmiRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("BMI record not found."));
        if (!record.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to delete this BMI record.");
        }
        bmiRecordRepository.delete(record);
    }
}
