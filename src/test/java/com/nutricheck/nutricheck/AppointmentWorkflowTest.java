package com.nutricheck.nutricheck;

import com.nutricheck.nutricheck.model.Appointment;
import com.nutricheck.nutricheck.model.AppointmentStatus;
import com.nutricheck.nutricheck.model.Role;
import com.nutricheck.nutricheck.model.User;
import com.nutricheck.nutricheck.repository.AppointmentRepository;
import com.nutricheck.nutricheck.repository.UserRepository;
import com.nutricheck.nutricheck.service.AppointmentService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AppointmentWorkflowTest {

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

    @BeforeEach
    public void setup() {
        patientJohn = userRepository.findByUsername("john_doe").orElseThrow();
        patientEmily = userRepository.findByUsername("emily_clark").orElseThrow();
        drSarah = userRepository.findByUsername("dr_sarah").orElseThrow();
    }

    @Test
    @DisplayName("Rule 1: Patient can cancel a PENDING appointment")
    public void testPatientCanCancelPendingAppointment() {
        Appointment appt = new Appointment(
                patientJohn,
                drSarah,
                LocalDate.now().plusDays(10),
                LocalTime.of(14, 0),
                "Initial consultation"
        );
        appt.setStatus(AppointmentStatus.PENDING);
        appt = appointmentRepository.save(appt);

        appointmentService.cancelAppointment(appt.getId(), patientJohn);

        Appointment updated = appointmentRepository.findById(appt.getId()).orElseThrow();
        assertEquals(AppointmentStatus.CANCELLED, updated.getStatus());
    }

    @Test
    @DisplayName("Rule 1: Patient can reschedule a PENDING appointment to an available slot")
    public void testPatientCanReschedulePendingAppointment() {
        Appointment appt = new Appointment(
                patientJohn,
                drSarah,
                LocalDate.now().plusDays(12),
                LocalTime.of(10, 0),
                "Initial pending appointment"
        );
        appt.setStatus(AppointmentStatus.PENDING);
        appt = appointmentRepository.save(appt);

        LocalDate newDate = LocalDate.now().plusDays(14);
        LocalTime newTime = LocalTime.of(16, 0);

        Appointment rescheduled = appointmentService.rescheduleAppointment(
                appt.getId(), newDate, newTime, "Updated health goals", patientJohn);

        assertEquals(newDate, rescheduled.getAppointmentDate());
        assertEquals(newTime, rescheduled.getAppointmentTime());
        assertEquals("Updated health goals", rescheduled.getPatientNotes());
    }

    @Test
    @DisplayName("Rule 2: Patient CANNOT cancel a CONFIRMED appointment")
    public void testPatientCannotCancelConfirmedAppointment() {
        Appointment appt = new Appointment(
                patientJohn,
                drSarah,
                LocalDate.now().plusDays(15),
                LocalTime.of(11, 0),
                "Confirmed session"
        );
        appt.setStatus(AppointmentStatus.CONFIRMED);
        appt = appointmentRepository.save(appt);

        Long apptId = appt.getId();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                appointmentService.cancelAppointment(apptId, patientJohn)
        );

        assertTrue(ex.getMessage().contains("Confirmed appointments cannot be cancelled by the patient"));
        Appointment stillConfirmed = appointmentRepository.findById(apptId).orElseThrow();
        assertEquals(AppointmentStatus.CONFIRMED, stillConfirmed.getStatus());
    }

    @Test
    @DisplayName("Rule 2: Patient CANNOT reschedule a CONFIRMED appointment")
    public void testPatientCannotRescheduleConfirmedAppointment() {
        Appointment appt = new Appointment(
                patientJohn,
                drSarah,
                LocalDate.now().plusDays(16),
                LocalTime.of(15, 0),
                "Confirmed session"
        );
        appt.setStatus(AppointmentStatus.CONFIRMED);
        appt = appointmentRepository.save(appt);

        Long apptId = appt.getId();
        LocalDate newDate = LocalDate.now().plusDays(18);
        LocalTime newTime = LocalTime.of(10, 0);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                appointmentService.rescheduleAppointment(apptId, newDate, newTime, "Notes", patientJohn)
        );

        assertTrue(ex.getMessage().contains("Confirmed appointments cannot be rescheduled by the patient"));
    }

    @Test
    @DisplayName("Rule 3: CANCELLED appointments cannot be modified or rescheduled")
    public void testCancelledAppointmentCannotBeRescheduled() {
        Appointment appt = new Appointment(
                patientJohn,
                drSarah,
                LocalDate.now().plusDays(20),
                LocalTime.of(9, 0),
                "Cancelled session"
        );
        appt.setStatus(AppointmentStatus.CANCELLED);
        appt = appointmentRepository.save(appt);

        Long apptId = appt.getId();
        LocalDate newDate = LocalDate.now().plusDays(22);
        LocalTime newTime = LocalTime.of(11, 0);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                appointmentService.rescheduleAppointment(apptId, newDate, newTime, "Notes", patientJohn)
        );

        assertTrue(ex.getMessage().contains("Cancelled appointments cannot be rescheduled"));
    }

    @Test
    @DisplayName("Rule 4: Rescheduling must detect nutritionist scheduling conflicts")
    public void testReschedulingDetectsNutritionistConflict() {
        // Create an existing booking for Dr. Sarah on Day +25 at 10:00 AM
        LocalDate conflictDate = LocalDate.now().plusDays(25);
        LocalTime conflictTime = LocalTime.of(10, 0);

        Appointment existingBooking = new Appointment(
                patientEmily,
                drSarah,
                conflictDate,
                conflictTime,
                "Emily's booking"
        );
        existingBooking.setStatus(AppointmentStatus.CONFIRMED);
        appointmentRepository.save(existingBooking);

        // John has a pending appointment on Day +24
        Appointment johnAppt = new Appointment(
                patientJohn,
                drSarah,
                LocalDate.now().plusDays(24),
                LocalTime.of(14, 0),
                "John's booking"
        );
        johnAppt.setStatus(AppointmentStatus.PENDING);
        johnAppt = appointmentRepository.save(johnAppt);

        Long johnApptId = johnAppt.getId();

        // John tries to reschedule to the same slot Emily already booked
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                appointmentService.rescheduleAppointment(johnApptId, conflictDate, conflictTime, "Try conflict slot", patientJohn)
        );

        assertTrue(ex.getMessage().contains("already booked"));
    }

    @Test
    @DisplayName("Rule 4: Rescheduling to a past date is blocked")
    public void testReschedulingToPastDateIsBlocked() {
        Appointment appt = new Appointment(
                patientJohn,
                drSarah,
                LocalDate.now().plusDays(5),
                LocalTime.of(10, 0),
                "Pending session"
        );
        appt.setStatus(AppointmentStatus.PENDING);
        appt = appointmentRepository.save(appt);

        Long apptId = appt.getId();
        LocalDate pastDate = LocalDate.now().minusDays(1);
        LocalTime time = LocalTime.of(10, 0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                appointmentService.rescheduleAppointment(apptId, pastDate, time, "Past date", patientJohn)
        );

        assertTrue(ex.getMessage().contains("Cannot reschedule to a past date"));
    }

    @Test
    @DisplayName("Rule 5: Patient CANNOT modify or cancel another patient's appointment (Ownership)")
    public void testPatientCannotModifyAnotherPatientsAppointment() {
        // Appointment belongs to Emily
        Appointment emilyAppt = new Appointment(
                patientEmily,
                drSarah,
                LocalDate.now().plusDays(30),
                LocalTime.of(11, 0),
                "Emily's private booking"
        );
        emilyAppt.setStatus(AppointmentStatus.PENDING);
        emilyAppt = appointmentRepository.save(emilyAppt);

        Long emilyApptId = emilyAppt.getId();

        // John attempts to cancel Emily's appointment
        assertThrows(SecurityException.class, () ->
                appointmentService.cancelAppointment(emilyApptId, patientJohn)
        );

        // John attempts to reschedule Emily's appointment
        assertThrows(SecurityException.class, () ->
                appointmentService.rescheduleAppointment(emilyApptId, LocalDate.now().plusDays(31), LocalTime.of(14, 0), "Hijack", patientJohn)
        );
    }

    @Test
    @DisplayName("Controller Test: POST /patient/appointments/reschedule/{id} as patient enforces rules with flash messages")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    public void testPatientControllerRescheduleEndpoint() throws Exception {
        Appointment appt = new Appointment(
                patientJohn,
                drSarah,
                LocalDate.now().plusDays(35),
                LocalTime.of(10, 0),
                "Controller test booking"
        );
        appt.setStatus(AppointmentStatus.PENDING);
        appt = appointmentRepository.save(appt);

        LocalDate targetDate = LocalDate.now().plusDays(36);

        mockMvc.perform(post("/patient/appointments/reschedule/" + appt.getId())
                .with(csrf())
                .param("appointmentDate", targetDate.toString())
                .param("appointmentTime", "14:00")
                .param("notes", "Rescheduled via controller"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/appointments"))
                .andExpect(flash().attributeExists("successMessage"));

        Appointment updated = appointmentRepository.findById(appt.getId()).orElseThrow();
        assertEquals(targetDate, updated.getAppointmentDate());
        assertEquals(LocalTime.of(14, 0), updated.getAppointmentTime());
    }

    @Test
    @DisplayName("View Test: GET /patient/appointments renders successfully without template errors")
    @WithMockUser(username = "john_doe", roles = {"PATIENT"})
    public void testGetPatientAppointmentsPageRendersSuccessfully() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/patient/appointments"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nutritionist Appointments")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("rescheduleModal")));
    }
}
