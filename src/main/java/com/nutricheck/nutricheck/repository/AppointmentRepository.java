package com.nutricheck.nutricheck.repository;

import com.nutricheck.nutricheck.model.Appointment;
import com.nutricheck.nutricheck.model.AppointmentStatus;
import com.nutricheck.nutricheck.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientOrderByAppointmentDateDescAppointmentTimeDesc(User patient);

    List<Appointment> findByNutritionistOrderByAppointmentDateDescAppointmentTimeDesc(User nutritionist);

    List<Appointment> findByNutritionistAndAppointmentDate(User nutritionist, LocalDate date);

    void deleteByPatient(User patient);

    void deleteByNutritionist(User nutritionist);

    boolean existsByNutritionistAndAppointmentDateAndAppointmentTimeAndStatusNot(
            User nutritionist, LocalDate appointmentDate, LocalTime appointmentTime, AppointmentStatus status);

    boolean existsByNutritionistAndAppointmentDateAndAppointmentTimeAndStatusNotAndIdNot(
            User nutritionist, LocalDate appointmentDate, LocalTime appointmentTime, AppointmentStatus status, Long id);

    long countByStatus(AppointmentStatus status);

    @Query("SELECT a.status, COUNT(a) FROM Appointment a GROUP BY a.status")
    List<Object[]> countGroupByStatus();
}
