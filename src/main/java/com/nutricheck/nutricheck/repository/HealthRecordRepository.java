package com.nutricheck.nutricheck.repository;

import com.nutricheck.nutricheck.model.HealthRecord;
import com.nutricheck.nutricheck.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {

    List<HealthRecord> findByUserOrderByRecordedAtDesc(User user);

    Optional<HealthRecord> findFirstByUserOrderByRecordedAtDesc(User user);

    long countByUser(User user);
    void deleteByUser(User user);

}
