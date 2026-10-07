package com.nutricheck.nutricheck.service;

import com.nutricheck.nutricheck.model.Appointment;
import com.nutricheck.nutricheck.model.AppointmentStatus;
import com.nutricheck.nutricheck.model.NotificationType;
import com.nutricheck.nutricheck.model.Role;
import com.nutricheck.nutricheck.model.User;
import com.nutricheck.nutricheck.repository.AppointmentRepository;
import com.nutricheck.nutricheck.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              UserRepository userRepository,
                              NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Appointment bookAppointment(User patient, Long nutritionistId, LocalDate date, LocalTime time, String notes) {
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot schedule an appointment in the past.");
        }

        User nutritionist = userRepository.findById(nutritionistId)
                .orElseThrow(() -> new IllegalArgumentException("Selected nutritionist not found."));

        // Check for booking conflict / duplicate time slot
        boolean conflict = appointmentRepository.existsByNutritionistAndAppointmentDateAndAppointmentTimeAndStatusNot(
                nutritionist, date, time, AppointmentStatus.CANCELLED);

        if (conflict) {
            throw new IllegalArgumentException("The nutritionist is already booked at " + time + " on " + date + ". Please choose another slot.");
        }

        Appointment appointment = new Appointment(patient, nutritionist, date, time, notes);
        Appointment saved = appointmentRepository.save(appointment);

        // 1. Patient books an appointment -> relevant nutritionist receives notification
        notificationService.createNotification(
                nutritionist,
                "New Consultation Request",
                "Patient " + patient.getFullName() + " has scheduled a consultation for " + date + " at " + time + ".",
                NotificationType.APPOINTMENT
        );

        return saved;
    }

    public List<Appointment> getAppointmentsForPatient(User patient) {
        return appointmentRepository.findByPatientOrderByAppointmentDateDescAppointmentTimeDesc(patient);
    }

    public List<Appointment> getAppointmentsForNutritionist(User nutritionist) {
        return appointmentRepository.findByNutritionistOrderByAppointmentDateDescAppointmentTimeDesc(nutritionist);
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    public Optional<Appointment> getAppointmentById(Long id) {
        return appointmentRepository.findById(id);
    }

    @Transactional
    public Appointment updateStatus(Long appointmentId, AppointmentStatus newStatus, String consultationNotes, User actor) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + appointmentId));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED && actor.getRole() != Role.ROLE_ADMIN) {
            throw new IllegalStateException("Cancelled appointments cannot be modified.");
        }

        AppointmentStatus previousStatus = appointment.getStatus();
        appointment.setStatus(newStatus);
        if (consultationNotes != null && !consultationNotes.trim().isEmpty()) {
            appointment.setConsultationNotes(consultationNotes.trim());
        }

        Appointment saved = appointmentRepository.save(appointment);

        // 2. Nutritionist / Admin confirms appointment -> patient receives notification
        if (newStatus == AppointmentStatus.CONFIRMED && previousStatus != AppointmentStatus.CONFIRMED) {
            notificationService.createNotification(
                    appointment.getPatient(),
                    "Consultation Confirmed",
                    "Your appointment with " + appointment.getNutritionist().getFullName() + " on " + appointment.getAppointmentDate() + " at " + appointment.getAppointmentTime() + " has been confirmed.",
                    NotificationType.APPOINTMENT
            );
        } else if (newStatus == AppointmentStatus.CANCELLED && previousStatus != AppointmentStatus.CANCELLED) {
            boolean actorIsPatient = appointment.getPatient().getId().equals(actor.getId());
            User recipient = actorIsPatient ? appointment.getNutritionist() : appointment.getPatient();
            notificationService.createNotification(
                    recipient,
                    "Consultation Cancelled",
                    "The consultation on " + appointment.getAppointmentDate() + " at " + appointment.getAppointmentTime() + " has been cancelled.",
                    NotificationType.APPOINTMENT
            );
        }

        return saved;
    }

    @Transactional
    public void cancelAppointment(Long appointmentId, User currentUser) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + appointmentId));

        boolean isPatient = appointment.getPatient().getId().equals(currentUser.getId());
        boolean isNutritionist = appointment.getNutritionist().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole().name().equals("ROLE_ADMIN");

        if (!isPatient && !isNutritionist && !isAdmin) {
            throw new SecurityException("Unauthorized: You can only cancel your own appointments.");
        }

        // Rule 3: CANCELLED appointments cannot be modified
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("This appointment is already cancelled.");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("Completed appointments cannot be cancelled.");
        }

        // Rule 2: Patient cannot cancel a CONFIRMED appointment
        if (isPatient && !isAdmin && !isNutritionist) {
            if (appointment.getStatus() == AppointmentStatus.CONFIRMED) {
                throw new IllegalStateException("Confirmed appointments cannot be cancelled by the patient. Please contact your nutritionist or clinic administrator.");
            }
            if (appointment.getStatus() != AppointmentStatus.PENDING) {
                throw new IllegalStateException("Only pending appointments can be cancelled by the patient.");
            }
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);

        // 3. Patient/nutritionist cancels an appointment -> relevant user receives notification
        User recipient = isPatient ? appointment.getNutritionist() : appointment.getPatient();
        String cancellerName = isPatient ? "Patient " + appointment.getPatient().getFullName() : "Your nutritionist";
        notificationService.createNotification(
                recipient,
                "Consultation Cancelled",
                cancellerName + " has cancelled the consultation scheduled for " + appointment.getAppointmentDate() + " at " + appointment.getAppointmentTime() + ".",
                NotificationType.APPOINTMENT
        );
    }

    @Transactional
    public Appointment rescheduleAppointment(Long appointmentId, LocalDate newDate, LocalTime newTime, String newNotes, User currentUser) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + appointmentId));

        boolean isPatient = appointment.getPatient().getId().equals(currentUser.getId());
        boolean isNutritionist = appointment.getNutritionist().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole().name().equals("ROLE_ADMIN");

        // Rule 5: Patient can only modify their own appointments
        if (!isPatient && !isNutritionist && !isAdmin) {
            throw new SecurityException("Unauthorized: You can only modify your own appointments.");
        }

        // Rule 3: CANCELLED appointments cannot be modified
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled appointments cannot be rescheduled or modified.");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("Completed appointments cannot be rescheduled.");
        }

        // Rule 2: Patient cannot reschedule a CONFIRMED appointment
        if (isPatient && !isAdmin && !isNutritionist) {
            if (appointment.getStatus() == AppointmentStatus.CONFIRMED) {
                throw new IllegalStateException("Confirmed appointments cannot be rescheduled by the patient. Please contact your nutritionist or clinic administrator.");
            }
            if (appointment.getStatus() != AppointmentStatus.PENDING) {
                throw new IllegalStateException("Only pending appointments can be rescheduled by the patient.");
            }
        }

        // Date validation: past dates not allowed
        if (newDate == null || newDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot reschedule to a past date. Please select a valid upcoming date.");
        }

        if (newTime == null) {
            throw new IllegalArgumentException("Appointment time is required.");
        }

        // Rule 4: Rescheduling must check nutritionist availability/conflicts (excluding cancelled and this appointment)
        boolean conflict = appointmentRepository.existsByNutritionistAndAppointmentDateAndAppointmentTimeAndStatusNotAndIdNot(
                appointment.getNutritionist(), newDate, newTime, AppointmentStatus.CANCELLED, appointment.getId());

        if (conflict) {
            throw new IllegalArgumentException("The nutritionist is already booked at " + newTime + " on " + newDate + ". Please choose another available slot.");
        }

        appointment.setAppointmentDate(newDate);
        appointment.setAppointmentTime(newTime);
        if (newNotes != null && !newNotes.trim().isEmpty()) {
            appointment.setPatientNotes(newNotes.trim());
        }

        Appointment saved = appointmentRepository.save(appointment);

        // 4. Patient reschedules a PENDING appointment -> relevant nutritionist receives notification
        User recipient = isPatient ? appointment.getNutritionist() : appointment.getPatient();
        String actorName = isPatient ? "Patient " + appointment.getPatient().getFullName() : "Your nutritionist";
        notificationService.createNotification(
                recipient,
                "Consultation Rescheduled",
                actorName + " has rescheduled the consultation to " + newDate + " at " + newTime + ".",
                NotificationType.APPOINTMENT
        );

        return saved;
    }

    @Transactional
    public void deleteAppointment(Long appointmentId) {
        appointmentRepository.deleteById(appointmentId);
    }

    public long countTotalAppointments() {
        return appointmentRepository.count();
    }

    public long countByStatus(AppointmentStatus status) {
        return appointmentRepository.countByStatus(status);
    }
}
