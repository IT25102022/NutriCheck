package com.nutricheck.nutricheck.service;

import com.nutricheck.nutricheck.model.Feedback;
import com.nutricheck.nutricheck.model.FeedbackStatus;
import com.nutricheck.nutricheck.model.Role;
import com.nutricheck.nutricheck.model.User;
import com.nutricheck.nutricheck.repository.FeedbackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    @Transactional
    public Feedback submitFeedback(User patient, String subject, String message) {
        if (patient == null) {
            throw new IllegalArgumentException("Patient information is required to submit feedback.");
        }

        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Feedback message cannot be empty. Please provide your comments.");
        }

        String sanitizedSubject = (subject != null && !subject.trim().isEmpty()) ? subject.trim() : "General Feedback";
        if (sanitizedSubject.length() > 150) {
            sanitizedSubject = sanitizedSubject.substring(0, 150);
        }

        Feedback feedback = new Feedback(patient, sanitizedSubject, message.trim());
        return feedbackRepository.save(feedback);
    }

    public List<Feedback> getFeedbackForPatient(User patient) {
        if (patient == null) {
            throw new IllegalArgumentException("Patient is required.");
        }
        return feedbackRepository.findByPatientOrderByCreatedAtDesc(patient);
    }

    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Feedback> getFeedbackByStatus(FeedbackStatus status) {
        if (status == null) {
            return getAllFeedback();
        }
        return feedbackRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public Optional<Feedback> getFeedbackById(Long id) {
        return feedbackRepository.findById(id);
    }

    @Transactional
    public Feedback updateFeedbackStatus(Long feedbackId, FeedbackStatus newStatus, String adminNotes, User actor) {
        if (actor == null || actor.getRole() != Role.ROLE_ADMIN) {
            throw new SecurityException("Unauthorized: Only administrators can update feedback status.");
        }

        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("Feedback record not found with id: " + feedbackId));

        if (newStatus != null) {
            feedback.setStatus(newStatus);
        }

        if (adminNotes != null) {
            feedback.setAdminNotes(adminNotes.trim().isEmpty() ? null : adminNotes.trim());
        }

        feedback.setUpdatedAt(LocalDateTime.now());
        return feedbackRepository.save(feedback);
    }

    @Transactional
    public void deleteFeedback(Long feedbackId, User patient) {
        if (patient == null) {
            throw new SecurityException("Authentication required to delete feedback.");
        }
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("Feedback record not found with id: " + feedbackId));

        if (feedback.getPatient() == null || !feedback.getPatient().getId().equals(patient.getId())) {
            throw new SecurityException("Unauthorized: You can only delete your own feedback.");
        }

        feedbackRepository.delete(feedback);
    }

    @Transactional
    public Feedback updateFeedback(Long feedbackId, String subject, String message, User patient) {
        if (patient == null) {
            throw new SecurityException("Authentication required to update feedback.");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Feedback message cannot be empty. Please provide your comments.");
        }
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("Feedback record not found with id: " + feedbackId));

        if (feedback.getPatient() == null || !feedback.getPatient().getId().equals(patient.getId())) {
            throw new SecurityException("Unauthorized: You can only update your own feedback.");
        }

        String sanitizedSubject = (subject != null && !subject.trim().isEmpty()) ? subject.trim() : "General Feedback";
        if (sanitizedSubject.length() > 150) {
            sanitizedSubject = sanitizedSubject.substring(0, 150);
        }

        feedback.setSubject(sanitizedSubject);
        feedback.setMessage(message.trim());
        feedback.setUpdatedAt(LocalDateTime.now());
        return feedbackRepository.save(feedback);
    }

    public long countTotalFeedback() {
        return feedbackRepository.count();
    }

    public long countByStatus(FeedbackStatus status) {
        return feedbackRepository.countByStatus(status);
    }
}
