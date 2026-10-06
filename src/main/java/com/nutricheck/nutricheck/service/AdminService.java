package com.nutricheck.nutricheck.service;

import com.nutricheck.nutricheck.model.AppointmentStatus;
import com.nutricheck.nutricheck.model.FeedbackStatus;
import com.nutricheck.nutricheck.model.Role;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AdminService {

    private final UserService userService;
    private final AppointmentService appointmentService;
    private final HealthRecordService healthRecordService;
    private final DietPlanService dietPlanService;
    private final BmiService bmiService;
    private final FeedbackService feedbackService;

    public AdminService(UserService userService,
                        AppointmentService appointmentService,
                        HealthRecordService healthRecordService,
                        DietPlanService dietPlanService,
                        BmiService bmiService,
                        FeedbackService feedbackService) {
        this.userService = userService;
        this.appointmentService = appointmentService;
        this.healthRecordService = healthRecordService;
        this.dietPlanService = dietPlanService;
        this.bmiService = bmiService;
        this.feedbackService = feedbackService;
    }

    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // User stats
        stats.put("totalUsers", userService.countTotalUsers());
        stats.put("totalPatients", userService.countByRole(Role.ROLE_PATIENT));
        stats.put("totalNutritionists", userService.countByRole(Role.ROLE_NUTRITIONIST));
        stats.put("totalHealthcareProfessionals", userService.countByRole(Role.ROLE_HEALTHCARE_PROFESSIONAL));
        stats.put("totalAdmins", userService.countByRole(Role.ROLE_ADMIN));

        // Appointments stats
        stats.put("totalAppointments", appointmentService.countTotalAppointments());
        stats.put("pendingAppointments", appointmentService.countByStatus(AppointmentStatus.PENDING));
        stats.put("confirmedAppointments", appointmentService.countByStatus(AppointmentStatus.CONFIRMED));
        stats.put("completedAppointments", appointmentService.countByStatus(AppointmentStatus.COMPLETED));
        stats.put("cancelledAppointments", appointmentService.countByStatus(AppointmentStatus.CANCELLED));

        // Records & Plans stats
        stats.put("totalHealthRecords", healthRecordService.countTotalRecords());
        stats.put("totalDietPlans", dietPlanService.countTotalDietPlans());

        // Feedback stats
        stats.put("totalFeedback", feedbackService.countTotalFeedback());
        stats.put("newFeedback", feedbackService.countByStatus(FeedbackStatus.NEW));
        stats.put("reviewedFeedback", feedbackService.countByStatus(FeedbackStatus.REVIEWED));
        stats.put("resolvedFeedback", feedbackService.countByStatus(FeedbackStatus.RESOLVED));

        // BMI Distribution
        stats.put("bmiDistribution", bmiService.getBmiDistributionStats());

        return stats;
    }
}
