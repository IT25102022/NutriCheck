package com.nutricheck.nutricheck.repository;

import com.nutricheck.nutricheck.model.BmiCategory;
import com.nutricheck.nutricheck.model.DietCategory;
import com.nutricheck.nutricheck.model.DietPlan;
import com.nutricheck.nutricheck.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

import com.nutricheck.nutricheck.model.DietPlanStatus;
import org.springframework.data.repository.query.Param;

@Repository
public interface DietPlanRepository extends JpaRepository<DietPlan, Long> {

    List<DietPlan> findByTargetBmiCategory(BmiCategory category);

    List<DietPlan> findByCategory(DietCategory category);

    List<DietPlan> findByCategoryAndTargetBmiCategory(DietCategory category, BmiCategory targetBmiCategory);

    @Query("SELECT d FROM DietPlan d WHERE LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(d.shortDescription) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<DietPlan> searchByKeyword(@Param("keyword") String keyword);

    List<DietPlan> findByStatus(DietPlanStatus status);

    List<DietPlan> findByStatusOrderByCreatedAtDesc(DietPlanStatus status);

    List<DietPlan> findByTargetBmiCategoryAndStatus(BmiCategory category, DietPlanStatus status);

    List<DietPlan> findByCategoryAndStatus(DietCategory category, DietPlanStatus status);

    List<DietPlan> findByCategoryAndTargetBmiCategoryAndStatus(DietCategory category, BmiCategory targetBmiCategory, DietPlanStatus status);

    @Query("SELECT d FROM DietPlan d WHERE d.status = :status AND (LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(d.shortDescription) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<DietPlan> searchByKeywordAndStatus(@Param("keyword") String keyword, @Param("status") DietPlanStatus status);

    void deleteByCreatedBy(User createdBy);

}
