package com.nutricheck.nutricheck;

import com.nutricheck.nutricheck.model.Feedback;
import com.nutricheck.nutricheck.model.FeedbackStatus;
import com.nutricheck.nutricheck.model.User;
import com.nutricheck.nutricheck.repository.FeedbackRepository;
import com.nutricheck.nutricheck.repository.UserRepository;
import com.nutricheck.nutricheck.service.FeedbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class FeedbackWorkflowTest {

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    private User patientJohn;
    private User patientEmily;
    private User adminUser;

    @BeforeEach
    public void setup() {
        patientJohn = userRepository.findByUsername("john_doe").orElseThrow();
        patientEmily = userRepository.findByUsername("emily_clark").orElseThrow();
        adminUser = userRepository.findByUsername("admin").orElseThrow();
    }

    @Test
    @DisplayName("1. Patient can submit feedback successfully")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    public void testPatientCanSubmitFeedback() throws Exception {
        mockMvc.perform(post("/patient/feedback")
                        .with(csrf())
                        .param("subject", "Diet Plan UI Feature")
                        .param("message", "I love the new meal suggestions! Could you add calorie breakdown charts?"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/feedback"))
                .andExpect(flash().attributeExists("successMessage"));

        List<Feedback> userFeedbacks = feedbackRepository.findByPatientOrderByCreatedAtDesc(patientJohn);
        assertFalse(userFeedbacks.isEmpty(), "Feedback should be saved in database");
        Feedback latest = userFeedbacks.get(0);
        assertEquals("Diet Plan UI Feature", latest.getSubject());
        assertEquals("I love the new meal suggestions! Could you add calorie breakdown charts?", latest.getMessage());
        assertEquals(FeedbackStatus.NEW, latest.getStatus());
    }

    @Test
    @DisplayName("2. Feedback is strictly associated with logged-in patient")
    public void testFeedbackAssociatedWithLoggedInPatient() {
        Feedback fb = feedbackService.submitFeedback(patientEmily, "Appointment Booking", "Booking with Dr. Sarah was seamless!");
        assertNotNull(fb.getId(), "Feedback ID must be generated");
        assertEquals(patientEmily.getId(), fb.getPatient().getId(), "Feedback must belong to Emily");
        assertEquals(FeedbackStatus.NEW, fb.getStatus(), "Initial status must be NEW");
    }

    @Test
    @DisplayName("3. Patient can only retrieve their own feedback submissions")
    public void testPatientOnlyRetrievesOwnFeedback() {
        Feedback fbJohn = feedbackService.submitFeedback(patientJohn, "Topic John", "Message by John");
        Feedback fbEmily = feedbackService.submitFeedback(patientEmily, "Topic Emily", "Message by Emily");

        List<Feedback> johnList = feedbackService.getFeedbackForPatient(patientJohn);
        List<Feedback> emilyList = feedbackService.getFeedbackForPatient(patientEmily);

        assertTrue(johnList.stream().anyMatch(f -> f.getId().equals(fbJohn.getId())));
        assertFalse(johnList.stream().anyMatch(f -> f.getId().equals(fbEmily.getId())), "John must not see Emily's feedback");

        assertTrue(emilyList.stream().anyMatch(f -> f.getId().equals(fbEmily.getId())));
        assertFalse(emilyList.stream().anyMatch(f -> f.getId().equals(fbJohn.getId())), "Emily must not see John's feedback");
    }

    @Test
    @DisplayName("4. Admin can view all submitted feedback")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testAdminCanViewAllFeedback() throws Exception {
        feedbackService.submitFeedback(patientJohn, "Admin Oversight Test", "Testing admin visibility");

        mockMvc.perform(get("/admin/feedback"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/feedback"))
                .andExpect(model().attributeExists("feedbackList"))
                .andExpect(model().attributeExists("statuses"))
                .andExpect(model().attributeExists("totalCount"));
    }

    @Test
    @DisplayName("5. Admin can update feedback status to REVIEWED and RESOLVED with notes")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testAdminCanUpdateFeedbackStatus() throws Exception {
        Feedback fb = feedbackService.submitFeedback(patientJohn, "Feature Improvement", "Please support export to PDF.");

        // Update to REVIEWED
        mockMvc.perform(post("/admin/feedback/" + fb.getId() + "/status")
                        .with(csrf())
                        .param("status", "REVIEWED")
                        .param("adminNotes", "Assigned to the engineering team."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/feedback"))
                .andExpect(flash().attributeExists("successMessage"));

        Feedback reviewed = feedbackRepository.findById(fb.getId()).orElseThrow();
        assertEquals(FeedbackStatus.REVIEWED, reviewed.getStatus());
        assertEquals("Assigned to the engineering team.", reviewed.getAdminNotes());

        // Update to RESOLVED
        mockMvc.perform(post("/admin/feedback/" + fb.getId() + "/status")
                        .with(csrf())
                        .param("status", "RESOLVED")
                        .param("adminNotes", "Feature shipped in v2.4."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/feedback"));

        Feedback resolved = feedbackRepository.findById(fb.getId()).orElseThrow();
        assertEquals(FeedbackStatus.RESOLVED, resolved.getStatus());
        assertEquals("Feature shipped in v2.4.", resolved.getAdminNotes());
    }

    @Test
    @DisplayName("6. Non-admin users cannot access admin feedback management or update status")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    public void testNonAdminCannotAccessAdminFeedback() throws Exception {
        // Patient cannot access admin feedback page
        mockMvc.perform(get("/admin/feedback"))
                .andExpect(status().isForbidden());

        // Nutritionist cannot access admin feedback page
        // Tested via programmatic service call
        Feedback fb = feedbackService.submitFeedback(patientJohn, "Security check", "Checking role barrier");
        assertThrows(SecurityException.class, () -> {
            feedbackService.updateFeedbackStatus(fb.getId(), FeedbackStatus.RESOLVED, "Illegal update", patientJohn);
        }, "Patient cannot call updateFeedbackStatus directly");
    }

    @Test
    @DisplayName("7. Empty or blank feedback is rejected")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    public void testEmptyFeedbackIsRejected() throws Exception {
        long countBefore = feedbackRepository.count();

        mockMvc.perform(post("/patient/feedback")
                        .with(csrf())
                        .param("subject", "Empty Subject Test")
                        .param("message", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/feedback"))
                .andExpect(flash().attributeExists("errorMessage"));

        long countAfter = feedbackRepository.count();
        assertEquals(countBefore, countAfter, "No feedback record should be created for blank message");

        assertThrows(IllegalArgumentException.class, () -> {
            feedbackService.submitFeedback(patientJohn, "Subject", "");
        }, "Empty message must throw IllegalArgumentException");
    }

    @Test
    @DisplayName("8. Patient can delete their own feedback via endpoint and service")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    public void testPatientCanDeleteOwnFeedback() throws Exception {
        Feedback fb = feedbackService.submitFeedback(patientJohn, "Temporary Subject", "Temporary message to delete");
        Long id = fb.getId();
        assertTrue(feedbackRepository.existsById(id));

        mockMvc.perform(post("/patient/feedback/" + id + "/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/feedback"))
                .andExpect(flash().attributeExists("successMessage"));

        assertFalse(feedbackRepository.existsById(id), "Feedback must be deleted from database");
    }

    @Test
    @DisplayName("9. Patient cannot delete another patient's feedback")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    public void testPatientCannotDeleteAnotherPatientsFeedback() throws Exception {
        Feedback emilyFb = feedbackService.submitFeedback(patientEmily, "Emily's Note", "Emily's private feedback");
        Long id = emilyFb.getId();

        // Attempt deletion by john_doe via HTTP endpoint
        mockMvc.perform(post("/patient/feedback/" + id + "/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/feedback"))
                .andExpect(flash().attributeExists("errorMessage"));

        assertTrue(feedbackRepository.existsById(id), "Emily's feedback must NOT be deleted");

        // Attempt programmatic deletion with mismatched owner
        assertThrows(SecurityException.class, () -> {
            feedbackService.deleteFeedback(id, patientJohn);
        }, "SecurityException must be thrown when attempting to delete another patient's feedback");
    }

    @Test
    @DisplayName("10. Patient can update their own feedback and cannot update another patient's feedback")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    public void testPatientUpdateFeedbackOwnership() throws Exception {
        Feedback fb = feedbackService.submitFeedback(patientJohn, "Original Subject", "Original Message");
        Long johnFbId = fb.getId();

        // Update own feedback
        mockMvc.perform(post("/patient/feedback/" + johnFbId + "/update")
                        .with(csrf())
                        .param("subject", "Updated Subject")
                        .param("message", "Updated Message Content"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/feedback"))
                .andExpect(flash().attributeExists("successMessage"));

        Feedback updated = feedbackRepository.findById(johnFbId).orElseThrow();
        assertEquals("Updated Subject", updated.getSubject());
        assertEquals("Updated Message Content", updated.getMessage());

        // Attempt updating Emily's feedback
        Feedback emilyFb = feedbackService.submitFeedback(patientEmily, "Emily Subject", "Emily Message");
        mockMvc.perform(post("/patient/feedback/" + emilyFb.getId() + "/update")
                        .with(csrf())
                        .param("subject", "Tampered Subject")
                        .param("message", "Tampered Message"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/feedback"))
                .andExpect(flash().attributeExists("errorMessage"));

        assertThrows(SecurityException.class, () -> {
            feedbackService.updateFeedback(emilyFb.getId(), "Tampered", "Tampered", patientJohn);
        });
    }
}
