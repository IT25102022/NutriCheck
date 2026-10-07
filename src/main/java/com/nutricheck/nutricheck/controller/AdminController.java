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

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final AppointmentService appointmentService;
    private final DietPlanService dietPlanService;
    private final HealthRecordService healthRecordService;
    private final BmiService bmiService;
    private final FeedbackService feedbackService;
    private final NotificationService notificationService;

    public AdminController(AdminService adminService,
                           UserService userService,
                           AppointmentService appointmentService,
                           DietPlanService dietPlanService,
                           HealthRecordService healthRecordService,
                           BmiService bmiService,
                           FeedbackService feedbackService,
                           NotificationService notificationService) {
        this.adminService = adminService;
        this.userService = userService;
        this.appointmentService = appointmentService;
        this.dietPlanService = dietPlanService;
        this.healthRecordService = healthRecordService;
        this.bmiService = bmiService;
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
        User admin = getAuthenticatedUser(userDetails);
        model.addAttribute("admin", admin);
        model.addAttribute("stats", adminService.getSystemStatistics());
        model.addAttribute("recentUsers", userService.getAllUsers());
        model.addAttribute("recentAppointments", appointmentService.getAllAppointments());
        model.addAttribute("recentHealthRecords", healthRecordService.getAllHealthRecords());
        model.addAttribute("dietPlans", dietPlanService.getAllDietPlans());
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(@AuthenticationPrincipal UserDetails userDetails,
                        @RequestParam(value = "query", required = false) String query,
                        @RequestParam(value = "role", required = false) Role roleFilter,
                        Model model) {
        User admin = getAuthenticatedUser(userDetails);
        List<User> userList;
        if (query != null && !query.trim().isEmpty()) {
            userList = userService.searchUsers(query);
        } else if (roleFilter != null) {
            userList = userService.getAllUsers().stream()
                    .filter(u -> u.getRole() == roleFilter)
                    .toList();
        } else {
            userList = userService.getAllUsers();
        }

        model.addAttribute("admin", admin);
        model.addAttribute("users", userList);
        model.addAttribute("roles", Role.values());
        model.addAttribute("query", query);
        model.addAttribute("selectedRole", roleFilter);
        model.addAttribute("nutritionistCount", userService.countByRole(Role.ROLE_NUTRITIONIST));
        model.addAttribute("healthcareProfessionalCount", userService.countByRole(Role.ROLE_HEALTHCARE_PROFESSIONAL));
        model.addAttribute("patientCount", userService.countByRole(Role.ROLE_PATIENT));
        model.addAttribute("adminCount", userService.countByRole(Role.ROLE_ADMIN));
        if (!model.containsAttribute("newUser")) {
            model.addAttribute("newUser", new User());
        }
        return "admin/users";
    }

    @PostMapping("/users/create-healthcare-professional")
    public String createHealthcareProfessional(@RequestParam("fullName") String fullName,
                                               @RequestParam("username") String username,
                                               @RequestParam("email") String email,
                                               @RequestParam("password") String password,
                                               @RequestParam(value = "phone", required = false) String phone,
                                               @RequestParam(value = "licenseNumber", required = false) String licenseNumber,
                                               @RequestParam(value = "specialization", required = false) String specialization,
                                               @RequestParam(value = "status", required = false, defaultValue = "ACTIVE") String status,
                                               RedirectAttributes redirectAttributes) {
        try {
            userService.createHealthcareProfessional(fullName, username, email, password, phone, licenseNumber, specialization, status);
            redirectAttributes.addFlashAttribute("successMessage", "Healthcare Professional profile created for " + fullName + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users?role=ROLE_HEALTHCARE_PROFESSIONAL";
    }

    @PostMapping("/users/create-nutritionist")
    public String createNutritionist(@RequestParam("fullName") String fullName,
                                     @RequestParam("username") String username,
                                     @RequestParam("email") String email,
                                     @RequestParam("password") String password,
                                     @RequestParam(value = "phone", required = false) String phone,
                                     @RequestParam(value = "licenseNumber", required = false) String licenseNumber,
                                     @RequestParam(value = "specialization", required = false) String specialization,
                                     RedirectAttributes redirectAttributes) {
        try {
            userService.createNutritionist(fullName, username, email, password, phone, licenseNumber, specialization);
            redirectAttributes.addFlashAttribute("successMessage", "Clinical Nutritionist profile created for " + fullName + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users?role=ROLE_NUTRITIONIST";
    }

    @PostMapping("/users/create-admin")
    public String createAdminAccount(@RequestParam("fullName") String fullName,
                                     @RequestParam("username") String username,
                                     @RequestParam("email") String email,
                                     @RequestParam("password") String password,
                                     @RequestParam(value = "phone", required = false) String phone,
                                     RedirectAttributes redirectAttributes) {
        try {
            userService.createAdmin(fullName, username, email, password, phone);
            redirectAttributes.addFlashAttribute("successMessage", "Administrator account created for " + fullName + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users?role=ROLE_ADMIN";
    }

    @PostMapping("/users/{id}/status")
    public String toggleStatus(@PathVariable("id") Long id,
                               @RequestParam("status") String status,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        User admin = getAuthenticatedUser(userDetails);
        if (admin.getId().equals(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You cannot deactivate your own admin account.");
            return "redirect:/admin/users";
        }

        try {
            userService.updateUserStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Account status updated to " + status + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String changeRole(@PathVariable("id") Long id,
                             @RequestParam("role") Role role,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.updateUserRole(id, role);
            redirectAttributes.addFlashAttribute("successMessage", "User role updated to " + role.getDisplayName() + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable("id") Long id,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        User admin = getAuthenticatedUser(userDetails);
        if (admin.getId().equals(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "You cannot delete your own admin account.");
            return "redirect:/admin/users";
        }

        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "User account removed from the system.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/appointments")
    public String appointments(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User admin = getAuthenticatedUser(userDetails);
        model.addAttribute("admin", admin);
        model.addAttribute("appointments", appointmentService.getAllAppointments());
        model.addAttribute("statuses", AppointmentStatus.values());
        return "admin/appointments";
    }

    @PostMapping("/appointments/{id}/status")
    public String updateAppointmentStatus(@PathVariable("id") Long id,
                                          @AuthenticationPrincipal UserDetails userDetails,
                                          @RequestParam("status") AppointmentStatus status,
                                          @RequestParam(value = "consultationNotes", required = false) String consultationNotes,
                                          RedirectAttributes redirectAttributes) {
        User admin = getAuthenticatedUser(userDetails);
        try {
            appointmentService.updateStatus(id, status, consultationNotes, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment status updated to " + status.getDisplayName() + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/appointments";
    }

    @PostMapping("/appointments/{id}/delete")
    public String deleteAppointment(@PathVariable("id") Long id,
                                    RedirectAttributes redirectAttributes) {
        try {
            appointmentService.deleteAppointment(id);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment record deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/appointments";
    }

    @GetMapping("/diet-plans")
    public String dietPlans(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User admin = getAuthenticatedUser(userDetails);
        model.addAttribute("admin", admin);
        model.addAttribute("plans", dietPlanService.getAllDietPlans());
        return "admin/diet-plans";
    }

    @PostMapping("/diet-plans/{id}/delete")
    public String deleteDietPlan(@PathVariable("id") Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            dietPlanService.deleteDietPlan(id);
            redirectAttributes.addFlashAttribute("successMessage", "Diet plan deleted from catalog.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/diet-plans";
    }

    @GetMapping("/health-records")
    public String healthRecords(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User admin = getAuthenticatedUser(userDetails);
        model.addAttribute("admin", admin);
        model.addAttribute("records", healthRecordService.getAllHealthRecords());
        return "admin/health-records";
    }

    @PostMapping("/health-records/{id}/delete")
    public String deleteHealthRecord(@PathVariable("id") Long id,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        User admin = getAuthenticatedUser(userDetails);
        try {
            healthRecordService.deleteHealthRecord(id, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Health record removed by administrator.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/health-records";
    }

    @GetMapping("/feedback")
    public String feedback(@AuthenticationPrincipal UserDetails userDetails,
                          @RequestParam(value = "status", required = false) FeedbackStatus status,
                          Model model) {
        User admin = getAuthenticatedUser(userDetails);
        List<Feedback> feedbackList = feedbackService.getFeedbackByStatus(status);

        model.addAttribute("admin", admin);
        model.addAttribute("feedbackList", feedbackList);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", FeedbackStatus.values());
        model.addAttribute("totalCount", feedbackService.countTotalFeedback());
        model.addAttribute("newCount", feedbackService.countByStatus(FeedbackStatus.NEW));
        model.addAttribute("reviewedCount", feedbackService.countByStatus(FeedbackStatus.REVIEWED));
        model.addAttribute("resolvedCount", feedbackService.countByStatus(FeedbackStatus.RESOLVED));
        return "admin/feedback";
    }

    @PostMapping("/feedback/{id}/status")
    public String updateFeedbackStatus(@PathVariable("id") Long id,
                                       @RequestParam("status") FeedbackStatus status,
                                       @RequestParam(value = "adminNotes", required = false) String adminNotes,
                                       @AuthenticationPrincipal UserDetails userDetails,
                                       RedirectAttributes redirectAttributes) {
        User admin = getAuthenticatedUser(userDetails);
        try {
            feedbackService.updateFeedbackStatus(id, status, adminNotes, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Feedback status updated to " + status.getDisplayName() + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/feedback";
    }

    @GetMapping("/notifications")
    public String notifications(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User admin = getAuthenticatedUser(userDetails);
        model.addAttribute("admin", admin);
        model.addAttribute("notifications", notificationService.getAllNotifications());
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("types", NotificationType.values());
        model.addAttribute("totalCount", notificationService.countTotalNotifications());
        model.addAttribute("unreadCount", notificationService.countUnreadNotifications());
        return "admin/notifications";
    }

    @PostMapping("/notifications/create")
    public String createNotification(@RequestParam("targetType") String targetType,
                                     @RequestParam(value = "userId", required = false) Long userId,
                                     @RequestParam("title") String title,
                                     @RequestParam("message") String message,
                                     @RequestParam("type") NotificationType type,
                                     RedirectAttributes redirectAttributes) {
        try {
            if ("USER".equalsIgnoreCase(targetType)) {
                if (userId == null) {
                    throw new IllegalArgumentException("Please select a specific recipient user.");
                }
                User recipient = userService.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("Target recipient not found."));
                notificationService.createNotification(recipient, title, message, type);
                redirectAttributes.addFlashAttribute("successMessage", "Notification sent to " + recipient.getFullName() + ".");
            } else if ("PATIENTS".equalsIgnoreCase(targetType)) {
                List<Notification> list = notificationService.createNotificationForRole(Role.ROLE_PATIENT, title, message, type);
                redirectAttributes.addFlashAttribute("successMessage", "Broadcast sent to " + list.size() + " patients.");
            } else if ("NUTRITIONISTS".equalsIgnoreCase(targetType)) {
                List<Notification> list = notificationService.createNotificationForRole(Role.ROLE_NUTRITIONIST, title, message, type);
                redirectAttributes.addFlashAttribute("successMessage", "Broadcast sent to " + list.size() + " nutritionists.");
            } else if ("ALL".equalsIgnoreCase(targetType)) {
                List<Notification> list = notificationService.createNotificationForAll(title, message, type);
                redirectAttributes.addFlashAttribute("successMessage", "Broadcast sent to all " + list.size() + " active users.");
            } else {
                throw new IllegalArgumentException("Invalid target selection.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/notifications";
    }

    @PostMapping("/notifications/{id}/read")
    public String markNotificationRead(@PathVariable("id") Long id,
                                       @AuthenticationPrincipal UserDetails userDetails,
                                       RedirectAttributes redirectAttributes) {
        User admin = getAuthenticatedUser(userDetails);
        try {
            notificationService.markAsRead(id, admin);
            redirectAttributes.addFlashAttribute("successMessage", "Notification marked as read.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/notifications";
    }
}

