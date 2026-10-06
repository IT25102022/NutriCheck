package com.nutricheck.nutricheck;

import com.nutricheck.nutricheck.model.*;
import com.nutricheck.nutricheck.repository.AppointmentRepository;
import com.nutricheck.nutricheck.repository.UserRepository;
import com.nutricheck.nutricheck.service.AppointmentService;
import com.nutricheck.nutricheck.service.NotificationService;
import com.nutricheck.nutricheck.service.TemporaryNotificationStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class NotificationWorkflowTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TemporaryNotificationStore temporaryNotificationStore;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    private User patientJohn;
    private User patientEmily;
    private User drSarah;
    private User adminUser;

    @BeforeEach
    public void setup() {
        notificationService.clearAll();
        patientJohn = userRepository.findByUsername("john_doe").orElseThrow();
        patientEmily = userRepository.findByUsername("emily_clark").orElseThrow();
        drSarah = userRepository.findByUsername("dr_sarah").orElseThrow();
        adminUser = userRepository.findByUsername("admin").orElseThrow();
    }

    @Test
    @DisplayName("1. Notification is created in temporary in-memory store for correct recipient")
    public void testNotificationCreatedForCorrectRecipient() {
        Notification n = notificationService.createNotification(
                patientEmily,
                "Health Goal Reminder",
                "Don't forget to log your daily water intake.",
                NotificationType.CLINICAL
        );

        assertNotNull(n.getId());
        assertEquals(patientEmily.getId(), n.getRecipient().getId());
        assertEquals("Health Goal Reminder", n.getTitle());
        assertFalse(n.isRead());

        List<Notification> emilyList = notificationService.getNotificationsForUser(patientEmily);
        assertTrue(emilyList.stream().anyMatch(item -> item.getId().equals(n.getId())));

        List<Notification> johnList = notificationService.getNotificationsForUser(patientJohn);
        assertFalse(johnList.stream().anyMatch(item -> item.getId().equals(n.getId())), "John must not receive Emily's notification");
    }

    @Test
    @DisplayName("2. Patient cannot see another patient's notification")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    public void testPatientCannotSeeAnotherPatientNotification() throws Exception {
        Notification emilyNotification = notificationService.createNotification(
                patientEmily,
                "Private Notification",
                "Private clinical data for Emily",
                NotificationType.CLINICAL
        );

        mockMvc.perform(get("/patient/notifications"))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/notifications"))
                .andExpect(model().attributeExists("notifications"));

        List<Notification> johnFeed = notificationService.getNotificationsForUser(patientJohn);
        assertFalse(johnFeed.stream().anyMatch(n -> n.getId().equals(emilyNotification.getId())));
    }

    @Test
    @DisplayName("3. Nutritionist cannot see another user's private notification")
    @WithMockUser(username = "dr_sarah", roles = {"NUTRITIONIST"})
    public void testNutritionistCannotSeeAnotherUserNotification() throws Exception {
        Notification johnNotification = notificationService.createNotification(
                patientJohn,
                "John Vitals Alert",
                "High blood pressure detected.",
                NotificationType.CLINICAL
        );

        mockMvc.perform(get("/nutritionist/notifications"))
                .andExpect(status().isOk())
                .andExpect(view().name("nutritionist/notifications"));

        List<Notification> drSarahFeed = notificationService.getNotificationsForUser(drSarah);
        assertFalse(drSarahFeed.stream().anyMatch(n -> n.getId().equals(johnNotification.getId())));
    }

    @Test
    @DisplayName("4. Admin can create targeted and broadcast notifications")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testAdminCanCreateTargetedAndBroadcastNotifications() throws Exception {
        // Create targeted notification for an individual user
        mockMvc.perform(post("/admin/notifications/create")
                        .with(csrf())
                        .param("targetType", "USER")
                        .param("userId", patientJohn.getId().toString())
                        .param("title", "Targeted Admin Alert")
                        .param("message", "Targeted message for John.")
                        .param("type", "SYSTEM"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/notifications"))
                .andExpect(flash().attributeExists("successMessage"));

        List<Notification> johnList = notificationService.getNotificationsForUser(patientJohn);
        assertTrue(johnList.stream().anyMatch(n -> n.getTitle().equals("Targeted Admin Alert")));

        // Admin broadcasts to all patients
        mockMvc.perform(post("/admin/notifications/create")
                        .with(csrf())
                        .param("targetType", "PATIENTS")
                        .param("title", "Broadcast to Patients")
                        .param("message", "System maintenance on Sunday.")
                        .param("type", "ANNOUNCEMENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/notifications"))
                .andExpect(flash().attributeExists("successMessage"));

        List<Notification> emilyList = notificationService.getNotificationsForUser(patientEmily);
        assertTrue(emilyList.stream().anyMatch(n -> n.getTitle().equals("Broadcast to Patients")));
    }

    @Test
    @DisplayName("5. Appointment booking creates notification for nutritionist")
    public void testAppointmentBookingCreatesNutritionistNotification() {
        long drSarahCountBefore = notificationService.getUnreadCountForUser(drSarah);

        LocalDate futureDate = LocalDate.now().plusDays(25);
        appointmentService.bookAppointment(patientJohn, drSarah.getId(), futureDate, LocalTime.of(9, 0), "Initial consult");

        long drSarahCountAfter = notificationService.getUnreadCountForUser(drSarah);
        assertEquals(drSarahCountBefore + 1, drSarahCountAfter, "Dr. Sarah must receive 1 new booking notification");

        List<Notification> drFeed = notificationService.getNotificationsForUser(drSarah);
        Notification latest = drFeed.get(0);
        assertEquals(NotificationType.APPOINTMENT, latest.getType());
        assertTrue(latest.getMessage().contains(patientJohn.getFullName()));
    }

    @Test
    @DisplayName("6. Appointment confirmation creates patient notification")
    public void testAppointmentConfirmationCreatesPatientNotification() {
        LocalDate futureDate = LocalDate.now().plusDays(26);
        Appointment appt = appointmentService.bookAppointment(patientEmily, drSarah.getId(), futureDate, LocalTime.of(10, 0), "Checkup");

        long emilyCountBefore = notificationService.getUnreadCountForUser(patientEmily);

        // Nutritionist confirms appointment
        appointmentService.updateStatus(appt.getId(), AppointmentStatus.CONFIRMED, "Confirmed slot.", drSarah);

        long emilyCountAfter = notificationService.getUnreadCountForUser(patientEmily);
        assertEquals(emilyCountBefore + 1, emilyCountAfter, "Emily must receive 1 confirmation notification");

        List<Notification> emilyFeed = notificationService.getNotificationsForUser(patientEmily);
        Notification latest = emilyFeed.get(0);
        assertEquals("Consultation Confirmed", latest.getTitle());
        assertTrue(latest.getMessage().contains(drSarah.getFullName()));
    }

    @Test
    @DisplayName("7. Appointment rescheduling creates notification for nutritionist")
    public void testAppointmentReschedulingCreatesNutritionistNotification() {
        LocalDate date1 = LocalDate.now().plusDays(27);
        LocalDate date2 = LocalDate.now().plusDays(28);
        Appointment appt = appointmentService.bookAppointment(patientJohn, drSarah.getId(), date1, LocalTime.of(11, 0), "Notes");

        long drSarahCountBefore = notificationService.getUnreadCountForUser(drSarah);

        appointmentService.rescheduleAppointment(appt.getId(), date2, LocalTime.of(14, 0), "Moved to afternoon", patientJohn);

        long drSarahCountAfter = notificationService.getUnreadCountForUser(drSarah);
        assertEquals(drSarahCountBefore + 1, drSarahCountAfter, "Dr. Sarah must receive 1 rescheduling notification");

        List<Notification> drFeed = notificationService.getNotificationsForUser(drSarah);
        Notification latest = drFeed.get(0);
        assertEquals("Consultation Rescheduled", latest.getTitle());
    }

    @Test
    @DisplayName("8. Appointment cancellation creates notification for counterparty")
    public void testAppointmentCancellationCreatesNotification() {
        LocalDate date = LocalDate.now().plusDays(29);
        Appointment appt = appointmentService.bookAppointment(patientJohn, drSarah.getId(), date, LocalTime.of(15, 0), "Notes");

        long drSarahCountBefore = notificationService.getUnreadCountForUser(drSarah);

        // Patient cancels
        appointmentService.cancelAppointment(appt.getId(), patientJohn);

        long drSarahCountAfter = notificationService.getUnreadCountForUser(drSarah);
        assertEquals(drSarahCountBefore + 1, drSarahCountAfter, "Dr. Sarah must receive 1 cancellation notification");

        List<Notification> drFeed = notificationService.getNotificationsForUser(drSarah);
        Notification latest = drFeed.get(0);
        assertEquals("Consultation Cancelled", latest.getTitle());
    }

    @Test
    @DisplayName("9. Unauthorized user cannot mark another user's notification as read")
    public void testUnauthorizedUserCannotManipulateNotification() {
        Notification emilyNotification = notificationService.createNotification(
                patientEmily,
                "Emily's Notice",
                "Content",
                NotificationType.SYSTEM
        );

        // John cannot mark Emily's notification as read
        assertThrows(SecurityException.class, () -> {
            notificationService.markAsRead(emilyNotification.getId(), patientJohn);
        });
    }

    @Test
    @DisplayName("10. Patient can mark own notification as read")
    public void testMarkAsReadWorks() {
        Notification n = notificationService.createNotification(patientJohn, "Unread Test", "Message", NotificationType.SYSTEM);
        assertFalse(n.isRead());

        Notification readNotif = notificationService.markAsRead(n.getId(), patientJohn);
        assertTrue(readNotif.isRead());
        assertNotNull(readNotif.getReadAt());
    }

    @Test
    @DisplayName("11. Notifications are immutable: content, recipient, type, and creation time are fixed")
    public void testNotificationImmutability() {
        Notification n = notificationService.createNotification(
                patientJohn,
                "Immutable Title",
                "Immutable Message Content",
                NotificationType.CLINICAL
        );

        assertNotNull(n.getId());
        assertEquals("Immutable Title", n.getTitle());
        assertEquals("Immutable Message Content", n.getMessage());
        assertEquals(NotificationType.CLINICAL, n.getType());
        assertEquals(patientJohn.getId(), n.getRecipient().getId());
        assertNotNull(n.getCreatedAt());

        // Marking as read only modifies read status and timestamp, nothing else
        Notification readNotif = notificationService.markAsRead(n.getId(), patientJohn);
        assertEquals("Immutable Title", readNotif.getTitle());
        assertEquals("Immutable Message Content", readNotif.getMessage());
        assertEquals(NotificationType.CLINICAL, readNotif.getType());
        assertEquals(n.getCreatedAt(), readNotif.getCreatedAt());
        assertTrue(readNotif.isRead());
        assertNotNull(readNotif.getReadAt());
    }
}
