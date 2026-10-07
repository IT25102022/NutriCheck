package com.nutricheck.nutricheck.service;

import com.nutricheck.nutricheck.model.Role;
import com.nutricheck.nutricheck.model.User;
import com.nutricheck.nutricheck.repository.UserRepository;
import com.nutricheck.nutricheck.repository.AppointmentRepository;
import com.nutricheck.nutricheck.repository.BmiRecordRepository;
import com.nutricheck.nutricheck.repository.DietPlanRepository;
import com.nutricheck.nutricheck.repository.FeedbackRepository;
import com.nutricheck.nutricheck.repository.HealthRecordRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final BmiRecordRepository bmiRecordRepository;
    private final DietPlanRepository dietPlanRepository;
    private final FeedbackRepository feedbackRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       AppointmentRepository appointmentRepository,
                       BmiRecordRepository bmiRecordRepository,
                       DietPlanRepository dietPlanRepository,
                       FeedbackRepository feedbackRepository,
                       HealthRecordRepository healthRecordRepository,
                       NotificationService notificationService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.bmiRecordRepository = bmiRecordRepository;
        this.dietPlanRepository = dietPlanRepository;
        this.feedbackRepository = feedbackRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.notificationService = notificationService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(User user, Role defaultRole) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username is already taken.");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email address is already registered.");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(defaultRole != null ? defaultRole : Role.ROLE_PATIENT);
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getAllNutritionists() {
        return userRepository.findByRole(Role.ROLE_NUTRITIONIST);
    }

    public List<User> getAllPatients() {
        return userRepository.findByRole(Role.ROLE_PATIENT);
    }

    public List<User> getAllHealthcareProfessionals() {
        return userRepository.findByRole(Role.ROLE_HEALTHCARE_PROFESSIONAL);
    }

    public List<User> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return userRepository.findAll();
        }
        return userRepository.searchUsers(query.trim());
    }

    @Transactional
    public User updateProfile(Long userId, String fullName, String phone, Integer age, String gender) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        user.setFullName(fullName);
        user.setPhone(phone);
        user.setAge(age);
        user.setGender(gender);
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password does not match our records.");
        }
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters long.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void updateUserRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        user.setRole(newRole);
        userRepository.save(user);
    }

    @Transactional
    public void updateUserStatus(Long userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        user.setStatus(status);
        userRepository.save(user);
    }

    @Transactional
    public User createNutritionist(String fullName, String username, String email, String password, String phone, String licenseNumber, String specialization) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username is already taken.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email address is already registered.");
        }
        if (password == null || password.trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }

        User nutritionist = new User(
                username.trim(),
                email.trim(),
                passwordEncoder.encode(password),
                fullName.trim(),
                phone != null ? phone.trim() : null,
                Role.ROLE_NUTRITIONIST,
                null,
                null,
                licenseNumber != null ? licenseNumber.trim() : null,
                specialization != null ? specialization.trim() : "General Clinical Nutrition"
        );
        return userRepository.save(nutritionist);
    }

    @Transactional
    public User createHealthcareProfessional(String fullName, String username, String email, String password,
                                             String phone, String licenseNumber, String specialization, String status) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username is already taken.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email address is already registered.");
        }
        if (password == null || password.trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }

        User hp = new User(
                username.trim(),
                email.trim(),
                passwordEncoder.encode(password),
                fullName.trim(),
                phone != null ? phone.trim() : null,
                Role.ROLE_HEALTHCARE_PROFESSIONAL,
                null,
                null,
                licenseNumber != null ? licenseNumber.trim() : null,
                specialization != null ? specialization.trim() : "Clinical Nutrition & Dietetics"
        );
        if (status != null && !status.trim().isEmpty()) {
            hp.setStatus(status.trim().toUpperCase());
        }
        return userRepository.save(hp);
    }

    @Transactional
    public User createAdmin(String fullName, String username, String email, String password, String phone) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username is already taken.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email address is already registered.");
        }
        if (password == null || password.trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }

        User admin = new User(
                username.trim(),
                email.trim(),
                passwordEncoder.encode(password),
                fullName.trim(),
                phone != null ? phone.trim() : null,
                Role.ROLE_ADMIN,
                null,
                null
        );
        return userRepository.save(admin);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        // Remove records that reference this user before deleting the user itself.
        // This preserves foreign-key integrity for patient/nutritionist relationships.
        appointmentRepository.deleteByPatient(user);
        appointmentRepository.deleteByNutritionist(user);
        bmiRecordRepository.deleteByUser(user);
        healthRecordRepository.deleteByUser(user);
        feedbackRepository.deleteByPatient(user);
        notificationService.clearNotificationsForUser(user);
        dietPlanRepository.deleteByCreatedBy(user);

        userRepository.delete(user);
    }

    public long countTotalUsers() {
        return userRepository.count();
    }

    public long countByRole(Role role) {
        return userRepository.countByRole(role);
    }
}
