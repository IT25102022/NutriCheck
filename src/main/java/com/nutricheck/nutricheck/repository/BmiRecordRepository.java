package com.nutricheck.nutricheck.repository;

import com.nutricheck.nutricheck.model.BmiCategory;
import com.nutricheck.nutricheck.model.BmiRecord;
import com.nutricheck.nutricheck.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BmiRecordRepository extends JpaRepository<BmiRecord, Long> {

    List<BmiRecord> findByUserOrderByRecordedAtAsc(User user);

    List<BmiRecord> findByUserOrderByRecordedAtDesc(User user);

    Optional<BmiRecord> findFirstByUserOrderByRecordedAtDesc(User user);

    long countByBmiCategory(BmiCategory category);

    @Query("SELECT r.bmiCategory, COUNT(r) FROM BmiRecord r GROUP BY r.bmiCategory")
    List<Object[]> countGroupByBmiCategory();
    void deleteByUser(User user);

}
