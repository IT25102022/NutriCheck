package com.nutricheck.nutricheck;

import com.nutricheck.nutricheck.model.Role;
import com.nutricheck.nutricheck.model.User;
import com.nutricheck.nutricheck.repository.UserRepository;
import com.nutricheck.nutricheck.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthAndSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("TEST 1 & 9: Public registration strictly forces role = PATIENT even if client injects role=ROLE_ADMIN")
    public void testPublicRegistrationForcesPatientRole() throws Exception {
        String testUsername = "new_patient_" + System.currentTimeMillis();
        String testEmail = testUsername + "@example.com";

        mockMvc.perform(post("/register")
                .with(csrf())
                .param("fullName", "Test Patient")
                .param("username", testUsername)
                .param("email", testEmail)
                .param("password", "securePass123")
                .param("role", "ROLE_ADMIN") // Attempt client tampering
                .param("assignedRole", "ROLE_ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        Optional<User> savedUser = userRepository.findByUsername(testUsername);
        assertTrue(savedUser.isPresent());
        assertEquals(Role.ROLE_PATIENT, savedUser.get().getRole(), "Backend must strictly force ROLE_PATIENT");
    }

    @Test
    @DisplayName("TEST 2: Patient login redirects to /patient/dashboard")
    public void testPatientLoginRedirectsToPatientDashboard() throws Exception {
        mockMvc.perform(formLogin("/login").user("john_doe").password("patient123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/dashboard"));
    }

    @Test
    @DisplayName("TEST 3 & 4: Patient cannot access Admin Dashboard or Admin APIs")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    public void testPatientCannotAccessAdminRoutes() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 5: Admin creates a nutritionist with clinical credentials")
    public void testAdminCreatesNutritionist() {
        String testNutrUsername = "nutr_staff_" + System.currentTimeMillis();
        String testNutrEmail = testNutrUsername + "@clinic.com";

        User nutr = userService.createNutritionist(
                "Dr. Test Staff, RD",
                testNutrUsername,
                testNutrEmail,
                "staffPass123",
                "+1 555-0188",
                "RD-99412",
                "Pediatric Nutrition"
        );

        assertNotNull(nutr.getId());
        assertEquals(Role.ROLE_NUTRITIONIST, nutr.getRole());
        assertEquals("RD-99412", nutr.getLicenseNumber());
        assertEquals("Pediatric Nutrition", nutr.getSpecialization());
    }

    @Test
    @DisplayName("TEST 6: Nutritionist login redirects to /nutritionist/dashboard")
    public void testNutritionistLoginRedirectsToNutritionistDashboard() throws Exception {
        mockMvc.perform(formLogin("/login").user("dr_sarah").password("nutritionist123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/nutritionist/dashboard"));
    }

    @Test
    @DisplayName("TEST 7: Nutritionist cannot access Admin Dashboard")
    @WithMockUser(username = "dr_sarah", roles = {"NUTRITIONIST"})
    public void testNutritionistCannotAccessAdminRoutes() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 8: Admin login redirects to /admin/dashboard")
    public void testAdminLoginRedirectsToAdminDashboard() throws Exception {
        mockMvc.perform(formLogin("/login").user("admin").password("admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    @DisplayName("TEST 10: Patient profile update cannot escalate privileges")
    public void testPatientProfileUpdateCannotEscalateRole() {
        User patient = userRepository.findByUsername("john_doe").orElseThrow();
        assertEquals(Role.ROLE_PATIENT, patient.getRole());

        userService.updateProfile(patient.getId(), "John Doe Updated", "+1 555-9999", 31, "Male");

        User updated = userRepository.findByUsername("john_doe").orElseThrow();
        assertEquals("John Doe Updated", updated.getFullName());
        assertEquals(Role.ROLE_PATIENT, updated.getRole(), "Role must remain ROLE_PATIENT after profile update");
    }

    @Test
    @DisplayName("TEST 11: Login page is production-grade and has no test/evaluation profiles")
    public void testLoginPageHasNoEvaluationProfiles() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Welcome to NutriCheck")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sign In to Portal")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Evaluation Profiles"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Admin Profile"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Nutritionist Profile"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Patient Profile"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("fillCredentials"))));
    }

    @Autowired
    private com.nutricheck.nutricheck.repository.BmiRecordRepository bmiRecordRepository;

    @Autowired
    private com.nutricheck.nutricheck.service.BmiService bmiService;

    @Test
    @DisplayName("TEST 12: Patient can create a BMI record directly via POST /patient/bmi-tracking")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    public void testPatientCanCreateBmiRecordDirectly() throws Exception {
        User patient = userRepository.findByUsername("john_doe").orElseThrow();
        long countBefore = bmiRecordRepository.findByUserOrderByRecordedAtAsc(patient).size();

        mockMvc.perform(post("/patient/bmi-tracking")
                        .with(csrf())
                        .param("heightCm", "175.0")
                        .param("weightKg", "70.0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/bmi-tracking"))
                .andExpect(flash().attributeExists("successMessage"));

        java.util.List<com.nutricheck.nutricheck.model.BmiRecord> records = bmiRecordRepository.findByUserOrderByRecordedAtAsc(patient);
        assertEquals(countBefore + 1, records.size());
        com.nutricheck.nutricheck.model.BmiRecord latest = records.get(records.size() - 1);
        assertEquals(175.0, latest.getHeightCm());
        assertEquals(70.0, latest.getWeightKg());
        assertEquals(22.86, latest.getBmiValue(), 0.05);
        assertEquals(com.nutricheck.nutricheck.model.BmiCategory.NORMAL, latest.getBmiCategory());
    }

    @Test
    @DisplayName("TEST 13: Invalid zero or negative height/weight returns error message on BMI creation")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    public void testPatientBmiCreationWithInvalidInputs() throws Exception {
        mockMvc.perform(post("/patient/bmi-tracking")
                        .with(csrf())
                        .param("heightCm", "-175.0")
                        .param("weightKg", "70.0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/bmi-tracking"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("TEST 14: Patient can edit own BMI record and recalculate BMI value")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    public void testPatientCanEditOwnBmiRecord() throws Exception {
        User patient = userRepository.findByUsername("john_doe").orElseThrow();
        com.nutricheck.nutricheck.model.BmiRecord record = bmiService.recordBmi(patient, 170.0, 65.0);

        mockMvc.perform(post("/patient/bmi-tracking/edit/" + record.getId())
                        .with(csrf())
                        .param("heightCm", "180.0")
                        .param("weightKg", "90.0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/bmi-tracking"))
                .andExpect(flash().attributeExists("successMessage"));

        com.nutricheck.nutricheck.model.BmiRecord updated = bmiRecordRepository.findById(record.getId()).orElseThrow();
        assertEquals(180.0, updated.getHeightCm());
        assertEquals(90.0, updated.getWeightKg());
        assertEquals(27.78, updated.getBmiValue(), 0.05);
        assertEquals(com.nutricheck.nutricheck.model.BmiCategory.OVERWEIGHT, updated.getBmiCategory());
    }

    @Test
    @DisplayName("TEST 15: Patient can delete own BMI record")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    public void testPatientCanDeleteOwnBmiRecord() throws Exception {
        User patient = userRepository.findByUsername("john_doe").orElseThrow();
        com.nutricheck.nutricheck.model.BmiRecord record = bmiService.recordBmi(patient, 175.0, 70.0);

        mockMvc.perform(post("/patient/bmi-tracking/delete/" + record.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/bmi-tracking"))
                .andExpect(flash().attributeExists("successMessage"));

        assertTrue(bmiRecordRepository.findById(record.getId()).isEmpty());
    }

    @Test
    @DisplayName("TEST 16: Patient cannot edit or delete another patient's BMI record")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    public void testPatientCannotEditOrDeleteAnotherPatientRecord() throws Exception {
        User emily = userRepository.findByUsername("emily_clark").orElseThrow();
        com.nutricheck.nutricheck.model.BmiRecord emilyRecord = bmiService.recordBmi(emily, 165.0, 58.0);

        // John attempts to edit Emily's record
        mockMvc.perform(post("/patient/bmi-tracking/edit/" + emilyRecord.getId())
                        .with(csrf())
                        .param("heightCm", "190.0")
                        .param("weightKg", "100.0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/bmi-tracking"))
                .andExpect(flash().attributeExists("errorMessage"));

        // John attempts to delete Emily's record
        mockMvc.perform(post("/patient/bmi-tracking/delete/" + emilyRecord.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/bmi-tracking"))
                .andExpect(flash().attributeExists("errorMessage"));

        // Emily's record must remain untouched
        com.nutricheck.nutricheck.model.BmiRecord safeRecord = bmiRecordRepository.findById(emilyRecord.getId()).orElseThrow();
        assertEquals(165.0, safeRecord.getHeightCm());
        assertEquals(58.0, safeRecord.getWeightKg());
    }
}
