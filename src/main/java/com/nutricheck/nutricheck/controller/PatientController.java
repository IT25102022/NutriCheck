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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/patient")
public class PatientController {

    private final UserService userService;
    private final HealthRecordService healthRecordService;
    private final BmiService bmiService;
    private final DietPlanService dietPlanService;
    private final AppointmentService appointmentService;
    private final FeedbackService feedbackService;
    private final NotificationService notificationService;

    public PatientController(UserService userService,
                             HealthRecordService healthRecordService,
                             BmiService bmiService,
                             DietPlanService dietPlanService,
                             AppointmentService appointmentService,
                             FeedbackService feedbackService,
                             NotificationService notificationService) {
        this.userService = userService;
        this.healthRecordService = healthRecordService;
        this.bmiService = bmiService;
        this.dietPlanService = dietPlanService;
        this.appointmentService = appointmentService;
        this.feedbackService = feedbackService;
        this.notificationService = notificationService;
    }

    private User getAuthenticatedUser(UserDetails userDetails) {
        return userService.findByUsername(userDetails.getUsername())
                .or(() -> userService.findByEmail(userDetails.getUsername()))
                .orElseThrow(() -> new IllegalStateException("User session not found"));
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getAuthenticatedUser(userDetails);
        model.addAttribute("user", user);

        Optional<HealthRecord> latestHealth = healthRecordService.getLatestRecordForUser(user);
        Optional<BmiRecord> latestBmi = bmiService.getLatestBmiRecord(user);
        List<BmiRecord> bmiHistory = bmiService.getUserBmiHistory(user);
        List<DietPlan> recommendedPlans = dietPlanService.getRecommendedDietPlansForUser(user);
        List<Appointment> upcomingAppointments = appointmentService.getAppointmentsForPatient(user);

        model.addAttribute("latestHealth", latestHealth.orElse(null));
        model.addAttribute("latestBmi", latestBmi.orElse(null));
        model.addAttribute("bmiHistory", bmiHistory);
        model.addAttribute("recommendedPlans", recommendedPlans);
        model.addAttribute("upcomingAppointments", upcomingAppointments);
        model.addAttribute("newHealthRecord", new HealthRecord());

        return "patient/dashboard";
    }

    @GetMapping("/health-records")
    public String healthRecords(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getAuthenticatedUser(userDetails);
        model.addAttribute("user", user);
        model.addAttribute("records", healthRecordService.getRecordsForUser(user));
        if (!model.containsAttribute("record")) {
            model.addAttribute("record", new HealthRecord());
        }
        return "patient/health-records";
    }

    @PostMapping("/health-records")
    public String addHealthRecord(@AuthenticationPrincipal UserDetails userDetails,
                                  @Valid @ModelAttribute("record") HealthRecord record,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        User user = getAuthenticatedUser(userDetails);
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            model.addAttribute("records", healthRecordService.getRecordsForUser(user));
            return "patient/health-records";
        }

        healthRecordService.saveHealthRecord(user, record);
        redirectAttributes.addFlashAttribute("successMessage", "Health record saved successfully!");
        return "redirect:/patient/health-records";
    }

    @GetMapping("/health-records/edit/{id}")
    public String editHealthRecordForm(@PathVariable("id") Long id,
                                       @AuthenticationPrincipal UserDetails userDetails,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        Optional<HealthRecord> record = healthRecordService.getRecordById(id);
        if (record.isEmpty() || !record.get().getUser().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Record not found or access denied.");
            return "redirect:/patient/health-records";
        }
        model.addAttribute("record", record.get());
        model.addAttribute("user", user);
        return "patient/health-record-edit";
    }

    @PostMapping("/health-records/edit/{id}")
    public String updateHealthRecord(@PathVariable("id") Long id,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     @Valid @ModelAttribute("record") HealthRecord record,
                                     BindingResult bindingResult,
                                     RedirectAttributes redirectAttributes,
                                     Model model) {
        User user = getAuthenticatedUser(userDetails);
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "patient/health-record-edit";
        }

        try {
            healthRecordService.updateHealthRecord(id, record, user);
            redirectAttributes.addFlashAttribute("successMessage", "Health record updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/health-records";
    }

    @PostMapping("/health-records/delete/{id}")
    public String deleteHealthRecord(@PathVariable("id") Long id,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        try {
            healthRecordService.deleteHealthRecord(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "Health record removed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/health-records";
    }

    @GetMapping("/bmi-tracking")
    public String bmiTracking(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getAuthenticatedUser(userDetails);
        List<BmiRecord> history = bmiService.getUserBmiHistory(user);
        Optional<BmiRecord> latest = bmiService.getLatestBmiRecord(user);

        model.addAttribute("user", user);
        model.addAttribute("bmiHistory", history);
        model.addAttribute("latestBmi", latest.orElse(null));
        model.addAttribute("bmiCategories", BmiCategory.values());
        return "patient/bmi-tracking";
    }

    @PostMapping("/bmi-tracking")
    public String addBmiRecord(@RequestParam("heightCm") Double heightCm,
                               @RequestParam("weightKg") Double weightKg,
                               @RequestParam(value = "age", required = false) Integer age,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        try {
            if (heightCm == null || heightCm <= 0 || weightKg == null || weightKg <= 0) {
                throw new IllegalArgumentException("Height and weight must be positive numbers.");
            }
            if (age == null) {
                age = user.getAge();
            }
            BmiRecord record = bmiService.recordBmi(user, heightCm, weightKg, age);
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("BMI measurement recorded successfully! (BMI: %.2f - %s)",
                            record.getBmiValue(), record.getBmiCategory().getDisplayName()));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/bmi-tracking";
    }

    @GetMapping("/bmi-tracking/edit/{id}")
    public String editBmiRecordForm(@PathVariable("id") Long id,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        Optional<BmiRecord> record = bmiService.getUserBmiHistory(user).stream()
                .filter(item -> item.getId().equals(id))
                .findFirst();

        if (record.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "BMI record not found or access denied.");
            return "redirect:/patient/bmi-tracking";
        }

        model.addAttribute("user", user);
        model.addAttribute("record", record.get());
        return "patient/bmi-record-edit";
    }

    @PostMapping("/bmi-tracking/edit/{id}")
    public String updateBmiRecord(@PathVariable("id") Long id,
                                  @RequestParam("heightCm") Double heightCm,
                                  @RequestParam("weightKg") Double weightKg,
                                  @RequestParam(value = "age", required = false) Integer age,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        try {
            bmiService.updateBmiRecord(id, heightCm, weightKg, age, user);
            redirectAttributes.addFlashAttribute("successMessage", "BMI measurement updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/bmi-tracking";
    }

    @PostMapping("/bmi-tracking/delete/{id}")
    public String deleteBmiRecord(@PathVariable("id") Long id,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        try {
            bmiService.deleteBmiRecord(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "BMI measurement removed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/bmi-tracking";
    }

    @GetMapping("/diet-plans")
    public String dietPlans(@AuthenticationPrincipal UserDetails userDetails,
                            @RequestParam(value = "category", required = false) DietCategory category,
                            @RequestParam(value = "bmiCategory", required = false) BmiCategory bmiCategory,
                            @RequestParam(value = "keyword", required = false) String keyword,
                            Model model) {
        User user = getAuthenticatedUser(userDetails);
        Optional<BmiRecord> latestBmi = bmiService.getLatestBmiRecord(user);
        BmiCategory userBmiCategory = latestBmi.map(BmiRecord::getBmiCategory).orElse(BmiCategory.NORMAL);

        List<DietPlan> plans;
        if (keyword != null && !keyword.trim().isEmpty()) {
            plans = dietPlanService.searchApprovedDietPlans(keyword);
        } else if (category != null && bmiCategory != null) {
            plans = dietPlanService.getApprovedDietPlansByCategoryAndBmiCategory(category, bmiCategory);
        } else if (bmiCategory != null) {
            plans = dietPlanService.getApprovedDietPlansByBmiCategory(bmiCategory);
        } else if (category != null) {
            plans = dietPlanService.getApprovedDietPlansByCategory(category);
        } else {
            plans = dietPlanService.getApprovedDietPlans();
        }

        model.addAttribute("user", user);
        model.addAttribute("plans", plans);
        model.addAttribute("userBmiCategory", userBmiCategory);
        model.addAttribute("dietCategories", DietCategory.values());
        model.addAttribute("bmiCategories", BmiCategory.values());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedBmiCategory", bmiCategory);
        model.addAttribute("keyword", keyword);

        return "patient/diet-plans";
    }

    @GetMapping("/appointments")
    public String appointments(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getAuthenticatedUser(userDetails);
        model.addAttribute("user", user);
        model.addAttribute("appointments", appointmentService.getAppointmentsForPatient(user));
        model.addAttribute("nutritionists", userService.getAllNutritionists());
        return "patient/appointments";
    }

    @PostMapping("/appointments/book")
    public String bookAppointment(@AuthenticationPrincipal UserDetails userDetails,
                                  @RequestParam("nutritionistId") Long nutritionistId,
                                  @RequestParam("appointmentDate") String dateStr,
                                  @RequestParam("appointmentTime") String timeStr,
                                  @RequestParam(value = "notes", required = false) String notes,
                                  RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        try {
            LocalDate date = LocalDate.parse(dateStr);
            LocalTime time = LocalTime.parse(timeStr);

            appointmentService.bookAppointment(user, nutritionistId, date, time, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment booked successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/appointments";
    }

    @PostMapping("/appointments/cancel/{id}")
    public String cancelAppointment(@PathVariable("id") Long id,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        try {
            appointmentService.cancelAppointment(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment cancelled successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/appointments";
    }

    @PostMapping("/appointments/reschedule/{id}")
    public String rescheduleAppointment(@PathVariable("id") Long id,
                                         @AuthenticationPrincipal UserDetails userDetails,
                                         @RequestParam("appointmentDate") String dateStr,
                                         @RequestParam("appointmentTime") String timeStr,
                                         @RequestParam(value = "notes", required = false) String notes,
                                         RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        try {
            LocalDate date = LocalDate.parse(dateStr);
            LocalTime time = LocalTime.parse(timeStr);

            appointmentService.rescheduleAppointment(id, date, time, notes, user);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment rescheduled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/appointments";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getAuthenticatedUser(userDetails);
        model.addAttribute("user", user);
        return "patient/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @RequestParam("fullName") String fullName,
                                @RequestParam("phone") String phone,
                                @RequestParam(value = "age", required = false) Integer age,
                                @RequestParam(value = "gender", required = false) String gender,
                                RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        try {
            userService.updateProfile(user.getId(), fullName, phone, age, gender);
            redirectAttributes.addFlashAttribute("successMessage", "Profile details updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        try {
            userService.changePassword(user.getId(), currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/profile";
    }

    @GetMapping("/feedback")
    public String feedback(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getAuthenticatedUser(userDetails);
        model.addAttribute("user", user);
        model.addAttribute("feedbackList", feedbackService.getFeedbackForPatient(user));
        return "patient/feedback";
    }

    @PostMapping("/feedback")
    public String submitFeedback(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam(value = "subject", required = false) String subject,
                                 @RequestParam("message") String message,
                                 RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        try {
            feedbackService.submitFeedback(user, subject, message);
            redirectAttributes.addFlashAttribute("successMessage", "Thank you! Your feedback has been submitted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/feedback";
    }

    @PostMapping("/feedback/{id}/delete")
    public String deleteFeedback(@PathVariable("id") Long id,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        try {
            feedbackService.deleteFeedback(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "Feedback entry deleted successfully.");
        } catch (SecurityException se) {
            redirectAttributes.addFlashAttribute("errorMessage", se.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/feedback";
    }

    @PostMapping("/feedback/{id}/update")
    public String updateFeedback(@PathVariable("id") Long id,
                                 @RequestParam(value = "subject", required = false) String subject,
                                 @RequestParam("message") String message,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        try {
            feedbackService.updateFeedback(id, subject, message, user);
            redirectAttributes.addFlashAttribute("successMessage", "Feedback updated successfully.");
        } catch (SecurityException se) {
            redirectAttributes.addFlashAttribute("errorMessage", se.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/feedback";
    }

    @GetMapping("/notifications")
    public String notifications(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getAuthenticatedUser(userDetails);
        model.addAttribute("user", user);
        model.addAttribute("notifications", notificationService.getNotificationsForUser(user));
        model.addAttribute("unreadCount", notificationService.getUnreadCountForUser(user));
        return "patient/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String markNotificationRead(@PathVariable("id") Long id,
                                       @AuthenticationPrincipal UserDetails userDetails,
                                       RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        try {
            notificationService.markAsRead(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "Notification marked as read.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllNotificationsRead(@AuthenticationPrincipal UserDetails userDetails,
                                           RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser(userDetails);
        try {
            notificationService.markAllAsRead(user);
            redirectAttributes.addFlashAttribute("successMessage", "All notifications marked as read.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/notifications";
    }
}
