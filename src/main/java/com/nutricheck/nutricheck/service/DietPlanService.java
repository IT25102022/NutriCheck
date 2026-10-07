package com.nutricheck.nutricheck.service;

import com.nutricheck.nutricheck.model.BmiCategory;
import com.nutricheck.nutricheck.model.BmiRecord;
import com.nutricheck.nutricheck.model.DietCategory;
import com.nutricheck.nutricheck.model.DietPlan;
import com.nutricheck.nutricheck.model.User;
import com.nutricheck.nutricheck.repository.DietPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import com.nutricheck.nutricheck.model.DietPlanStatus;
import com.nutricheck.nutricheck.model.Role;
import java.time.LocalDateTime;

@Service
public class DietPlanService {

    private final DietPlanRepository dietPlanRepository;
    private final BmiService bmiService;

    public DietPlanService(DietPlanRepository dietPlanRepository, BmiService bmiService) {
        this.dietPlanRepository = dietPlanRepository;
        this.bmiService = bmiService;
    }

    @Transactional
    public DietPlan createDietPlan(DietPlan dietPlan, User creator) {
        dietPlan.setCreatedBy(creator);
        if (dietPlan.getStatus() == null) {
            dietPlan.setStatus(DietPlanStatus.PENDING_APPROVAL);
        }
        return dietPlanRepository.save(dietPlan);
    }

    @Transactional
    public DietPlan updateDietPlan(Long id, DietPlan updatedData) {
        DietPlan plan = dietPlanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Diet plan not found with id: " + id));

        plan.setTitle(updatedData.getTitle());
        plan.setCategory(updatedData.getCategory());
        plan.setTargetBmiCategory(updatedData.getTargetBmiCategory());
        plan.setShortDescription(updatedData.getShortDescription());
        plan.setMealPlanDetails(updatedData.getMealPlanDetails());
        plan.setDailyCalories(updatedData.getDailyCalories());

        return dietPlanRepository.save(plan);
    }

    @Transactional
    public DietPlan approveDietPlan(Long id, User reviewer, String notes) {
        if (reviewer == null || (reviewer.getRole() != Role.ROLE_HEALTHCARE_PROFESSIONAL && reviewer.getRole() != Role.ROLE_ADMIN)) {
            throw new SecurityException("Unauthorized: Only Healthcare Professionals and Administrators can approve diet plans.");
        }
        DietPlan plan = dietPlanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Diet plan not found with id: " + id));

        plan.setStatus(DietPlanStatus.APPROVED);
        plan.setReviewedBy(reviewer);
        plan.setReviewNotes(notes != null && !notes.trim().isEmpty() ? notes.trim() : "Approved");
        plan.setReviewedAt(LocalDateTime.now());
        return dietPlanRepository.save(plan);
    }

    @Transactional
    public DietPlan rejectDietPlan(Long id, User reviewer, String rejectionReason) {
        if (reviewer == null || (reviewer.getRole() != Role.ROLE_HEALTHCARE_PROFESSIONAL && reviewer.getRole() != Role.ROLE_ADMIN)) {
            throw new SecurityException("Unauthorized: Only Healthcare Professionals and Administrators can reject diet plans.");
        }
        DietPlan plan = dietPlanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Diet plan not found with id: " + id));

        plan.setStatus(DietPlanStatus.REJECTED);
        plan.setReviewedBy(reviewer);
        plan.setReviewNotes(rejectionReason != null && !rejectionReason.trim().isEmpty() ? rejectionReason.trim() : "Rejected");
        plan.setReviewedAt(LocalDateTime.now());
        return dietPlanRepository.save(plan);
    }

    @Transactional
    public void deleteDietPlan(Long id) {
        dietPlanRepository.deleteById(id);
    }

    public List<DietPlan> getAllDietPlans() {
        return dietPlanRepository.findAll();
    }

    public List<DietPlan> getDietPlansByStatus(DietPlanStatus status) {
        return dietPlanRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public List<DietPlan> getPendingDietPlans() {
        return dietPlanRepository.findByStatusOrderByCreatedAtDesc(DietPlanStatus.PENDING_APPROVAL);
    }

    public List<DietPlan> getApprovedDietPlans() {
        return dietPlanRepository.findByStatusOrderByCreatedAtDesc(DietPlanStatus.APPROVED);
    }

    public List<DietPlan> getApprovedDietPlansByBmiCategory(BmiCategory category) {
        return dietPlanRepository.findByTargetBmiCategoryAndStatus(category, DietPlanStatus.APPROVED);
    }

    public List<DietPlan> getApprovedDietPlansByCategory(DietCategory category) {
        return dietPlanRepository.findByCategoryAndStatus(category, DietPlanStatus.APPROVED);
    }

    public List<DietPlan> getApprovedDietPlansByCategoryAndBmiCategory(DietCategory category, BmiCategory targetBmiCategory) {
        return dietPlanRepository.findByCategoryAndTargetBmiCategoryAndStatus(category, targetBmiCategory, DietPlanStatus.APPROVED);
    }

    public List<DietPlan> searchApprovedDietPlans(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getApprovedDietPlans();
        }
        return dietPlanRepository.searchByKeywordAndStatus(keyword.trim(), DietPlanStatus.APPROVED);
    }

    public Optional<DietPlan> getDietPlanById(Long id) {
        return dietPlanRepository.findById(id);
    }

    public List<DietPlan> getDietPlansByBmiCategory(BmiCategory category) {
        return dietPlanRepository.findByTargetBmiCategory(category);
    }

    public List<DietPlan> getDietPlansByCategory(DietCategory category) {
        return dietPlanRepository.findByCategory(category);
    }

    public List<DietPlan> searchDietPlans(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllDietPlans();
        }
        return dietPlanRepository.searchByKeyword(keyword.trim());
    }

    /**
     * Rule-based Recommendation Engine:
     * Suggests diet plans specifically matched to the user's latest BMI category.
     * Only APPROVED diet plans are recommended to patients.
     */
    public List<DietPlan> getRecommendedDietPlansForUser(User user) {
        Optional<BmiRecord> latestBmi = bmiService.getLatestBmiRecord(user);
        BmiCategory category = latestBmi.map(BmiRecord::getBmiCategory).orElse(BmiCategory.NORMAL);
        List<DietPlan> plans = dietPlanRepository.findByTargetBmiCategoryAndStatus(category, DietPlanStatus.APPROVED);
        if (plans.isEmpty()) {
            plans = dietPlanRepository.findByTargetBmiCategoryAndStatus(BmiCategory.NORMAL, DietPlanStatus.APPROVED);
        }
        return plans;
    }

    public long countTotalDietPlans() {
        return dietPlanRepository.count();
    }

    public long countByStatus(DietPlanStatus status) {
        return dietPlanRepository.findByStatus(status).size();
    }
}
