package com.nutricheck.nutricheck;

import com.nutricheck.nutricheck.model.*;
import com.nutricheck.nutricheck.repository.DietPlanRepository;
import com.nutricheck.nutricheck.repository.UserRepository;
import com.nutricheck.nutricheck.security.CustomUserDetailsService;
import com.nutricheck.nutricheck.service.DietPlanService;
import com.nutricheck.nutricheck.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class HealthcareProfessionalWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DietPlanService dietPlanService;

    @Autowired
    private DietPlanRepository dietPlanRepository;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User nutritionistUser;
    private User patientUser;
    private User healthcareProUser;

    @BeforeEach
    public void setup() {
        adminUser = userRepository.findByUsername("admin").orElseThrow();
        nutritionistUser = userRepository.findByUsername("dr_sarah").orElseThrow();
        patientUser = userRepository.findByUsername("john_doe").orElseThrow();

        if (userRepository.findByUsername("dr_alex").isEmpty()) {
            healthcareProUser = userService.createHealthcareProfessional(
                    "Dr. Alex Miller, MD",
                    "dr_alex",
                    "alex.miller@hospital.org",
                    "healthcare123",
                    "+1 555-9090",
                    "MD-99123",
                    "Clinical Nutrition & Metabolic Health",
                    "ACTIVE"
            );
        } else {
            healthcareProUser = userRepository.findByUsername("dr_alex").orElseThrow();
        }
    }

    @Test
    @DisplayName("1. Admin can create Healthcare Professional account via endpoint and service")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testAdminCanCreateHealthcareProfessionalAccount() throws Exception {
        String testUsername = "dr_claire_" + System.currentTimeMillis();
        String testEmail = testUsername + "@nutricheck.org";

        mockMvc.perform(post("/admin/users/create-healthcare-professional")
                        .with(csrf())
                        .param("fullName", "Dr. Claire Bennett")
                        .param("username", testUsername)
                        .param("email", testEmail)
                        .param("password", "securePass123")
                        .param("phone", "+1 555-8888")
                        .param("licenseNumber", "MD-55443")
                        .param("specialization", "Pediatric Clinical Nutrition")
                        .param("status", "ACTIVE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users?role=ROLE_HEALTHCARE_PROFESSIONAL"))
                .andExpect(flash().attributeExists("successMessage"));

        Optional<User> createdOpt = userRepository.findByUsername(testUsername);
        assertTrue(createdOpt.isPresent(), "Account must be created in the database");
        User created = createdOpt.get();
        assertEquals(Role.ROLE_HEALTHCARE_PROFESSIONAL, created.getRole());
        assertEquals("MD-55443", created.getLicenseNumber());
        assertEquals("Pediatric Clinical Nutrition", created.getSpecialization());
        assertEquals("ACTIVE", created.getStatus());
        assertTrue(passwordEncoder.matches("securePass123", created.getPassword()));
    }

    @Test
    @DisplayName("2. Healthcare Professional can log in through /login and redirect to dashboard")
    public void testHealthcareProfessionalCanLoginThroughForm() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "dr_alex")
                        .param("password", "healthcare123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/healthcare-professional/dashboard"));
    }

    @Test
    @DisplayName("3. Correct role ROLE_HEALTHCARE_PROFESSIONAL is loaded from the database")
    public void testCorrectRoleLoadedFromDatabase() {
        UserDetails userDetails = userDetailsService.loadUserByUsername("dr_alex");
        assertNotNull(userDetails);
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HEALTHCARE_PROFESSIONAL")));
        assertEquals(Role.ROLE_HEALTHCARE_PROFESSIONAL, healthcareProUser.getRole());
    }

    @Test
    @DisplayName("4. Healthcare Professional can access the correct dashboard")
    @WithMockUser(username = "dr_alex", roles = {"HEALTHCARE_PROFESSIONAL"})
    public void testHealthcareProfessionalCanAccessDashboard() throws Exception {
        mockMvc.perform(get("/healthcare-professional/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("healthcare-professional/dashboard"))
                .andExpect(model().attributeExists("professional"))
                .andExpect(model().attributeExists("pendingReviewCount"));
    }

    @Test
    @DisplayName("5. Healthcare Professional can perform permitted Nutritionist functionality")
    @WithMockUser(username = "dr_alex", roles = {"HEALTHCARE_PROFESSIONAL"})
    public void testHealthcareProfessionalPermittedNutritionistCapabilities() throws Exception {
        // Can access appointments schedule
        mockMvc.perform(get("/healthcare-professional/appointments"))
                .andExpect(status().isOk())
                .andExpect(view().name("healthcare-professional/appointments"));

        // Can access permitted nutritionist routes
        mockMvc.perform(get("/nutritionist/appointments"))
                .andExpect(status().isOk());

        // Can access patient health history
        mockMvc.perform(get("/healthcare-professional/patient/" + patientUser.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("healthcare-professional/patient-detail"))
                .andExpect(model().attributeExists("healthRecords"));
    }

    @Test
    @DisplayName("6. Healthcare Professional can approve a pending diet plan")
    @WithMockUser(username = "dr_alex", roles = {"HEALTHCARE_PROFESSIONAL"})
    public void testHealthcareProfessionalCanApprovePendingDietPlan() throws Exception {
        DietPlan plan = new DietPlan(
                "High Protein Post-Op Protocol",
                DietCategory.HEALTHY_LIVING,
                BmiCategory.NORMAL,
                "Support post-surgical lean mass retention.",
                "High protein balanced daily portions.",
                2200,
                nutritionistUser
        );
        DietPlan savedPlan = dietPlanService.createDietPlan(plan, nutritionistUser);
        assertEquals(DietPlanStatus.PENDING_APPROVAL, savedPlan.getStatus());

        mockMvc.perform(post("/healthcare-professional/diet-plans/" + savedPlan.getId() + "/approve")
                        .with(csrf())
                        .param("notes", "Clinically verified for lean mass recovery."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/healthcare-professional/diet-plans"))
                .andExpect(flash().attributeExists("successMessage"));

        DietPlan approved = dietPlanRepository.findById(savedPlan.getId()).orElseThrow();
        assertEquals(DietPlanStatus.APPROVED, approved.getStatus());
        assertEquals("Clinically verified for lean mass recovery.", approved.getReviewNotes());
        assertEquals(healthcareProUser.getId(), approved.getReviewedBy().getId());
        assertNotNull(approved.getReviewedAt());

        // Patient can see approved plans
        List<DietPlan> patientRecommendations = dietPlanService.getRecommendedDietPlansForUser(patientUser);
        assertTrue(patientRecommendations.stream().anyMatch(p -> p.getId().equals(approved.getId())));
    }

    @Test
    @DisplayName("7. Healthcare Professional can reject a pending diet plan with clinical reason")
    @WithMockUser(username = "dr_alex", roles = {"HEALTHCARE_PROFESSIONAL"})
    public void testHealthcareProfessionalCanRejectPendingDietPlan() throws Exception {
        DietPlan plan = new DietPlan(
                "Excessive Carb Weight Loss Plan",
                DietCategory.WEIGHT_LOSS,
                BmiCategory.OVERWEIGHT,
                "Inconsistent macro ratios.",
                "Plan details require revision.",
                1900,
                nutritionistUser
        );
        DietPlan savedPlan = dietPlanService.createDietPlan(plan, nutritionistUser);

        mockMvc.perform(post("/healthcare-professional/diet-plans/" + savedPlan.getId() + "/reject")
                        .with(csrf())
                        .param("reason", "Net carbohydrate calculation exceeds ketogenic threshold."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/healthcare-professional/diet-plans"))
                .andExpect(flash().attributeExists("successMessage"));

        DietPlan rejected = dietPlanRepository.findById(savedPlan.getId()).orElseThrow();
        assertEquals(DietPlanStatus.REJECTED, rejected.getStatus());
        assertEquals("Net carbohydrate calculation exceeds ketogenic threshold.", rejected.getReviewNotes());

        // Rejected plans are not released to patients
        List<DietPlan> patientRecommendations = dietPlanService.getRecommendedDietPlansForUser(patientUser);
        assertFalse(patientRecommendations.stream().anyMatch(p -> p.getId().equals(rejected.getId())));
    }

    @Test
    @DisplayName("8. Nutritionist cannot approve or reject diet plans")
    @WithMockUser(username = "dr_sarah", roles = {"NUTRITIONIST"})
    public void testNutritionistCannotApproveOrRejectDietPlan() throws Exception {
        DietPlan plan = new DietPlan(
                "Nutritionist Attempted Approval Test",
                DietCategory.WEIGHT_GAIN,
                BmiCategory.NORMAL,
                "Trial plan",
                "Meal details",
                2000,
                nutritionistUser
        );
        DietPlan savedPlan = dietPlanService.createDietPlan(plan, nutritionistUser);

        // Nutritionist cannot access Healthcare Professional approval endpoint
        mockMvc.perform(post("/healthcare-professional/diet-plans/" + savedPlan.getId() + "/approve")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        // Nutritionist calling service method directly throws SecurityException
        assertThrows(SecurityException.class, () -> {
            dietPlanService.approveDietPlan(savedPlan.getId(), nutritionistUser, "Self-approval attempt");
        }, "Nutritionist must not be authorized to approve diet plans");

        assertThrows(SecurityException.class, () -> {
            dietPlanService.rejectDietPlan(savedPlan.getId(), nutritionistUser, "Rejection attempt");
        }, "Nutritionist must not be authorized to reject diet plans");
    }

    @Test
    @DisplayName("9. Patient cannot approve or reject diet plans")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    public void testPatientCannotApproveOrRejectDietPlan() throws Exception {
        DietPlan plan = new DietPlan(
                "Patient Attempted Approval Test",
                DietCategory.HEALTHY_LIVING,
                BmiCategory.NORMAL,
                "Trial plan",
                "Meal details",
                1800,
                nutritionistUser
        );
        DietPlan savedPlan = dietPlanService.createDietPlan(plan, nutritionistUser);

        // Patient cannot access Healthcare Professional approval endpoint
        mockMvc.perform(post("/healthcare-professional/diet-plans/" + savedPlan.getId() + "/approve")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        // Patient calling service method directly throws SecurityException
        assertThrows(SecurityException.class, () -> {
            dietPlanService.approveDietPlan(savedPlan.getId(), patientUser, "Patient approval attempt");
        }, "Patient must not be authorized to approve diet plans");

        assertThrows(SecurityException.class, () -> {
            dietPlanService.rejectDietPlan(savedPlan.getId(), patientUser, "Patient rejection attempt");
        }, "Patient must not be authorized to reject diet plans");
    }

    @Test
    @DisplayName("10. Public registration cannot create Healthcare Professional accounts")
    public void testPublicRegistrationCannotCreateHealthcareProfessionalAccount() throws Exception {
        String attemptUsername = "tamper_hp_" + System.currentTimeMillis();

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("fullName", "Malicious User")
                        .param("username", attemptUsername)
                        .param("email", attemptUsername + "@example.com")
                        .param("password", "password123")
                        .param("role", "ROLE_HEALTHCARE_PROFESSIONAL")
                        .param("assignedRole", "ROLE_HEALTHCARE_PROFESSIONAL"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        Optional<User> savedUser = userRepository.findByUsername(attemptUsername);
        assertTrue(savedUser.isPresent());
        assertEquals(Role.ROLE_PATIENT, savedUser.get().getRole(),
                "Public registration must strictly force ROLE_PATIENT, ignoring any role tampering");
    }
}
