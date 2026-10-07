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
@RequestMapping("/nutritionist")
public class NutritionistController {

    private final UserService userService;
    private final AppointmentService appointmentService;
    private final DietPlanService dietPlanService;
    private final HealthRecordService healthRecordService;
    private final BmiService bmiService;
    private final NotificationService notificationService;

    public NutritionistController(UserService userService,
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
        User nutritionist = getAuthenticatedUser(userDetails);
        List<Appointment> myAppointments = appointmentService.getAppointmentsForNutritionist(nutritionist);
        List<DietPlan> allPlans = dietPlanService.getAllDietPlans();

        long pendingCount = myAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.PENDING).count();
        long confirmedCount = myAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED).count();
        long completedCount = myAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();

        model.addAttribute("nutritionist", nutritionist);
        model.addAttribute("appointments", myAppointments);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("confirmedCount", confirmedCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("dietPlans", allPlans);

        return "nutritionist/dashboard";
    }

    @GetMapping("/appointments")
    public String appointments(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User nutritionist = getAuthenticatedUser(userDetails);
        model.addAttribute("nutritionist", nutritionist);
        model.addAttribute("appointments", appointmentService.getAppointmentsForNutritionist(nutritionist));
        model.addAttribute("statuses", AppointmentStatus.values());
        return "nutritionist/appointments";
    }

    @PostMapping("/appointments/{id}/status")
    public String updateStatus(@PathVariable("id") Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam("status") AppointmentStatus status,
                               @RequestParam(value = "consultationNotes", required = false) String consultationNotes,
                               RedirectAttributes redirectAttributes) {
        User nutritionist = getAuthenticatedUser(userDetails);
        try {
            appointmentService.updateStatus(id, status, consultationNotes, nutritionist);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nutritionist/appointments";
    }

    @GetMapping("/patient/{id}")
    public String viewPatientHistory(@PathVariable("id") Long patientId,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        User nutritionist = getAuthenticatedUser(userDetails);
        Optional<User> patientOpt = userService.findById(patientId);
        if (patientOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Patient not found.");
            return "redirect:/nutritionist/appointments";
        }

        User patient = patientOpt.get();
        model.addAttribute("nutritionist", nutritionist);
        model.addAttribute("patient", patient);
        model.addAttribute("healthRecords", healthRecordService.getRecordsForUser(patient));
        model.addAttribute("bmiHistory", bmiService.getUserBmiHistory(patient));
        model.addAttribute("latestBmi", bmiService.getLatestBmiRecord(patient).orElse(null));
        model.addAttribute("appointments", appointmentService.getAppointmentsForPatient(patient));

        return "nutritionist/patient-detail";
    }

    @GetMapping("/diet-plans")
    public String dietPlans(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User nutritionist = getAuthenticatedUser(userDetails);
        model.addAttribute("nutritionist", nutritionist);
        model.addAttribute("plans", dietPlanService.getAllDietPlans());
        if (!model.containsAttribute("newPlan")) {
            model.addAttribute("newPlan", new DietPlan());
        }
        model.addAttribute("dietCategories", DietCategory.values());
        model.addAttribute("bmiCategories", BmiCategory.values());
        return "nutritionist/diet-plans";
    }

    @PostMapping("/diet-plans/create")
    public String createDietPlan(@AuthenticationPrincipal UserDetails userDetails,
                                 @Valid @ModelAttribute("newPlan") DietPlan plan,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        User nutritionist = getAuthenticatedUser(userDetails);
        if (bindingResult.hasErrors()) {
            model.addAttribute("nutritionist", nutritionist);
            model.addAttribute("plans", dietPlanService.getAllDietPlans());
            model.addAttribute("dietCategories", DietCategory.values());
            model.addAttribute("bmiCategories", BmiCategory.values());
            return "nutritionist/diet-plans";
        }

        dietPlanService.createDietPlan(plan, nutritionist);
        redirectAttributes.addFlashAttribute("successMessage", "New diet plan created successfully!");
        return "redirect:/nutritionist/diet-plans";
    }

    @GetMapping("/diet-plans/edit/{id}")
    public String editDietPlanForm(@PathVariable("id") Long id,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        User nutritionist = getAuthenticatedUser(userDetails);
        Optional<DietPlan> plan = dietPlanService.getDietPlanById(id);
        if (plan.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Diet plan not found.");
            return "redirect:/nutritionist/diet-plans";
        }

        model.addAttribute("nutritionist", nutritionist);
        model.addAttribute("plan", plan.get());
        model.addAttribute("dietCategories", DietCategory.values());
        model.addAttribute("bmiCategories", BmiCategory.values());
        return "nutritionist/diet-plan-edit";
    }

    @PostMapping("/diet-plans/edit/{id}")
    public String updateDietPlan(@PathVariable("id") Long id,
                                 @Valid @ModelAttribute("plan") DietPlan plan,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("nutritionist", getAuthenticatedUser(userDetails));
            model.addAttribute("dietCategories", DietCategory.values());
            model.addAttribute("bmiCategories", BmiCategory.values());
            return "nutritionist/diet-plan-edit";
        }

        try {
            dietPlanService.updateDietPlan(id, plan);
            redirectAttributes.addFlashAttribute("successMessage", "Diet plan updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nutritionist/diet-plans";
    }

    @PostMapping("/diet-plans/delete/{id}")
    public String deleteDietPlan(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            dietPlanService.deleteDietPlan(id);
            redirectAttributes.addFlashAttribute("successMessage", "Diet plan removed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nutritionist/diet-plans";
    }

    @GetMapping("/notifications")
    public String notifications(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User nutritionist = getAuthenticatedUser(userDetails);
        model.addAttribute("nutritionist", nutritionist);
        model.addAttribute("notifications", notificationService.getNotificationsForUser(nutritionist));
        model.addAttribute("unreadCount", notificationService.getUnreadCountForUser(nutritionist));
        return "nutritionist/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String markNotificationRead(@PathVariable("id") Long id,
                                       @AuthenticationPrincipal UserDetails userDetails,
                                       RedirectAttributes redirectAttributes) {
        User nutritionist = getAuthenticatedUser(userDetails);
        try {
            notificationService.markAsRead(id, nutritionist);
            redirectAttributes.addFlashAttribute("successMessage", "Notification marked as read.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nutritionist/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllNotificationsRead(@AuthenticationPrincipal UserDetails userDetails,
                                           RedirectAttributes redirectAttributes) {
        User nutritionist = getAuthenticatedUser(userDetails);
        try {
            notificationService.markAllAsRead(nutritionist);
            redirectAttributes.addFlashAttribute("successMessage", "All notifications marked as read.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/nutritionist/notifications";
    }
}
