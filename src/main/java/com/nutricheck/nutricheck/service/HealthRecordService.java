package com.nutricheck.nutricheck.service;

import com.nutricheck.nutricheck.model.HealthRecord;
import com.nutricheck.nutricheck.model.User;
import com.nutricheck.nutricheck.repository.HealthRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class HealthRecordService {

    private final HealthRecordRepository healthRecordRepository;

    public HealthRecordService(HealthRecordRepository healthRecordRepository) {
        this.healthRecordRepository = healthRecordRepository;
    }

    @Transactional
    public HealthRecord saveHealthRecord(User user, HealthRecord record) {
        record.setUser(user);
        return healthRecordRepository.save(record);
    }

    public List<HealthRecord> getRecordsForUser(User user) {
        return healthRecordRepository.findByUserOrderByRecordedAtDesc(user);
    }

    public List<HealthRecord> getAllHealthRecords() {
        return healthRecordRepository.findAll();
    }

    public Optional<HealthRecord> getRecordById(Long id) {
        return healthRecordRepository.findById(id);
    }

    public Optional<HealthRecord> getLatestRecordForUser(User user) {
        return healthRecordRepository.findFirstByUserOrderByRecordedAtDesc(user);
    }

    @Transactional
    public HealthRecord updateHealthRecord(Long id, HealthRecord updatedData, User currentUser) {
        HealthRecord record = healthRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Health record not found with id: " + id));

        // Authorization check
        if (!record.getUser().getId().equals(currentUser.getId()) && !currentUser.getRole().name().equals("ROLE_ADMIN")) {
            throw new SecurityException("Unauthorized to modify this health record.");
        }

        record.setHeightCm(updatedData.getHeightCm());
        record.setWeightKg(updatedData.getWeightKg());
        record.setSystolicBp(updatedData.getSystolicBp());
        record.setDiastolicBp(updatedData.getDiastolicBp());
        record.setBloodSugar(updatedData.getBloodSugar());
        record.setCholesterol(updatedData.getCholesterol());
        record.setNotes(updatedData.getNotes());

        return healthRecordRepository.save(record);
    }

    @Transactional
    public void deleteHealthRecord(Long id, User currentUser) {
        HealthRecord record = healthRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Health record not found with id: " + id));

        if (!record.getUser().getId().equals(currentUser.getId()) && !currentUser.getRole().name().equals("ROLE_ADMIN")) {
            throw new SecurityException("Unauthorized to delete this health record.");
        }

        healthRecordRepository.delete(record);
    }

    public long countTotalRecords() {
        return healthRecordRepository.count();
    }
}
