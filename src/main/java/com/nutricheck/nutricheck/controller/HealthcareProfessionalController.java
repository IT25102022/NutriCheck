package com.nutricheck.nutricheck.controller;

import com.nutricheck.nutricheck.model.*;
import com.nutricheck.nutricheck.service.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/healthcare-professional")
public class HealthcareProfessionalController {

    private final UserService userService;
    private final AppointmentService appointmentService;
    private final DietPlanService dietPlanService;
    private final HealthRecordService healthRecordService;
    private final BmiService bmiService;
    private final NotificationService notificationService;

    public HealthcareProfessionalController(UserService userService,
                                            AppointmentService appointmentService,
                                            DietPlanService dietPlanService,
                                            HealthRecordService healthRecordService,
                                            BmiService bmiService,
                                            NotificationService notificationService) {
        this.userService = userService;
        this.appointmentService = appointmentService;
        this.dietPlanService = dietPlanService;
        this.healthRecordService = healthRecordService;
        this.bmiService = bmiService;
        this.notificationService = notificationService;
    }

    private User getAuthenticatedUser(UserDetails userDetails) {
        return userService.findByUsername(userDetails.getUsername())
                .or(() -> userService.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> new IllegalStateException("User session not found"));
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User hp = getAuthenticatedUser(userDetails);
        List<DietPlan> pendingPlans = dietPlanService.getPendingDietPlans();
        List<Appointment> myAppointments = appointmentService.getAppointmentsForNutritionist(hp);

        long pendingReviewCount = pendingPlans.size();
        long approvedCount = dietPlanService.countByStatus(DietPlanStatus.APPROVED);
        long rejectedCount = dietPlanService.countByStatus(DietPlanStatus.REJECTED);
        long pendingAppointmentsCount = myAppointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.PENDING).count();

        model.addAttribute("professional", hp);
        model.addAttribute("pendingPlans", pendingPlans);
        model.addAttribute("pendingReviewCount", pendingReviewCount);
        model.addAttribute("approvedCount", approvedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("appointments", myAppointments);
        model.addAttribute("pendingAppointmentsCount", pendingAppointmentsCount);

        return "healthcare-professional/dashboard";
    }

    @GetMapping("/diet-plans")
    public String dietPlans(@AuthenticationPrincipal UserDetails userDetails,
                            @RequestParam(value = "status", required = false) DietPlanStatus statusFilter,
                            Model model) {
        User hp = getAuthenticatedUser(userDetails);
        List<DietPlan> plans;
        if (statusFilter != null) {
            plans = dietPlanService.getDietPlansByStatus(statusFilter);
        } else {
            plans = dietPlanService.getAllDietPlans();
        }

        model.addAttribute("professional", hp);
        model.addAttribute("plans", plans);
        model.addAttribute("selectedStatus", statusFilter);
        model.addAttribute("statuses", DietPlanStatus.values());
        model.addAttribute("pendingCount", dietPlanService.countByStatus(DietPlanStatus.PENDING_APPROVAL));
        model.addAttribute("approvedCount", dietPlanService.countByStatus(DietPlanStatus.APPROVED));
        model.addAttribute("rejectedCount", dietPlanService.countByStatus(DietPlanStatus.REJECTED));
        if (!model.containsAttribute("newPlan")) {
            model.addAttribute("newPlan", new DietPlan());
        }
        model.addAttribute("dietCategories", DietCategory.values());
        model.addAttribute("bmiCategories", BmiCategory.values());

        return "healthcare-professional/diet-plans";
    }

    @PostMapping("/diet-plans/{id}/approve")
    public String approveDietPlan(@PathVariable("id") Long id,
                                  @RequestParam(value = "notes", required = false) String notes,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        User hp = getAuthenticatedUser(userDetails);
        try {
            dietPlanService.approveDietPlan(id, hp, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Diet plan #" + id + " has been approved and is now active for patients.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/healthcare-professional/diet-plans";
    }

    @PostMapping("/diet-plans/{id}/reject")
    public String rejectDietPlan(@PathVariable("id") Long id,
                                 @RequestParam(value = "reason", required = false) String reason,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        User hp = getAuthenticatedUser(userDetails);
        try {
            dietPlanService.rejectDietPlan(id, hp, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Diet plan #" + id + " has been rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/healthcare-professional/diet-plans";
    }

    @PostMapping("/diet-plans/create")
    public String createDietPlan(@AuthenticationPrincipal UserDetails userDetails,
                                 @Valid @ModelAttribute("newPlan") DietPlan plan,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        User hp = getAuthenticatedUser(userDetails);
        if (bindingResult.hasErrors()) {
            model.addAttribute("professional", hp);
            model.addAttribute("plans", dietPlanService.getAllDietPlans());
            model.addAttribute("dietCategories", DietCategory.values());
            model.addAttribute("bmiCategories", BmiCategory.values());
            model.addAttribute("statuses", DietPlanStatus.values());
            return "healthcare-professional/diet-plans";
        }

        // When created by a Healthcare Professional, it can be saved and directly reviewed/approved
        plan.setStatus(DietPlanStatus.APPROVED);
        plan.setReviewedBy(hp);
        plan.setReviewNotes("Created and approved by Healthcare Professional");
        dietPlanService.createDietPlan(plan, hp);
        redirectAttributes.addFlashAttribute("successMessage", "Diet plan created and approved successfully!");
        return "redirect:/healthcare-professional/diet-plans";
    }

    @GetMapping("/appointments")
    public String appointments(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User hp = getAuthenticatedUser(userDetails);
        model.addAttribute("professional", hp);
        model.addAttribute("appointments", appointmentService.getAppointmentsForNutritionist(hp));
        model.addAttribute("statuses", AppointmentStatus.values());
        return "healthcare-professional/appointments";
    }

    @PostMapping("/appointments/{id}/status")
    public String updateAppointmentStatus(@PathVariable("id") Long id,
                                          @AuthenticationPrincipal UserDetails userDetails,
                                          @RequestParam("status") AppointmentStatus status,
                                          @RequestParam(value = "consultationNotes", required = false) String consultationNotes,
                                          RedirectAttributes redirectAttributes) {
        User hp = getAuthenticatedUser(userDetails);
        try {
            appointmentService.updateStatus(id, status, consultationNotes, hp);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/healthcare-professional/appointments";
    }

    @GetMapping("/patient/{id}")
    public String viewPatientHistory(@PathVariable("id") Long patientId,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        User hp = getAuthenticatedUser(userDetails);
        Optional<User> patientOpt = userService.findById(patientId);
        if (patientOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Patient not found.");
            return "redirect:/healthcare-professional/dashboard";
        }

        User patient = patientOpt.get();
        model.addAttribute("professional", hp);
        model.addAttribute("patient", patient);
        model.addAttribute("healthRecords", healthRecordService.getRecordsForUser(patient));
        model.addAttribute("bmiHistory", bmiService.getUserBmiHistory(patient));
        model.addAttribute("latestBmi", bmiService.getLatestBmiRecord(patient).orElse(null));
        model.addAttribute("appointments", appointmentService.getAppointmentsForPatient(patient));

        return "healthcare-professional/patient-detail";
    }

    @GetMapping("/notifications")
    public String notifications(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User hp = getAuthenticatedUser(userDetails);
        model.addAttribute("professional", hp);
        model.addAttribute("notifications", notificationService.getNotificationsForUser(hp));
        model.addAttribute("unreadCount", notificationService.getUnreadCountForUser(hp));
        return "healthcare-professional/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String markNotificationRead(@PathVariable("id") Long id,
                                       @AuthenticationPrincipal UserDetails userDetails,
                                       RedirectAttributes redirectAttributes) {
        User hp = getAuthenticatedUser(userDetails);
        try {
            notificationService.markAsRead(id, hp);
            redirectAttributes.addFlashAttribute("successMessage", "Notification marked as read.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/healthcare-professional/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllNotificationsRead(@AuthenticationPrincipal UserDetails userDetails,
                                           RedirectAttributes redirectAttributes) {
        User hp = getAuthenticatedUser(userDetails);
        try {
            notificationService.markAllAsRead(hp);
            redirectAttributes.addFlashAttribute("successMessage", "All notifications marked as read.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/healthcare-professional/notifications";
    }
}
