package com.nutricheck.nutricheck.repository;

import com.nutricheck.nutricheck.model.Feedback;
import com.nutricheck.nutricheck.model.FeedbackStatus;
import com.nutricheck.nutricheck.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByPatientOrderByCreatedAtDesc(User patient);

    List<Feedback> findAllByOrderByCreatedAtDesc();

    List<Feedback> findByStatusOrderByCreatedAtDesc(FeedbackStatus status);

    long countByStatus(FeedbackStatus status);
    void deleteByPatient(User patient);

}
