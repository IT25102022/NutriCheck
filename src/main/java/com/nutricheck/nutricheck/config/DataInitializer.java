package com.nutricheck.nutricheck.config;

import com.nutricheck.nutricheck.model.*;
import com.nutricheck.nutricheck.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Value("${nutricheck.admin.username:}")
    private String adminUsername;

    @Value("${nutricheck.admin.email:}")
    private String adminEmail;

    @Value("${nutricheck.admin.password:}")
    private String adminPassword;

    @Value("${nutricheck.admin.full-name:System Administrator}")
    private String adminFullName;

    @Value("${nutricheck.admin.phone:+1 555-0199}")
    private String adminPhone;

    @Value("${nutricheck.seed.sample-data:false}")
    private boolean seedSampleData;


    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this(userRepository, passwordEncoder, null);
    }

    @Autowired
    public DataInitializer(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        removeObsoleteNotificationTable();
        provisionInitialAdmin();
        if (seedSampleData) {
            seedSampleData();
        }
    }

    /**
     * Safely drops the obsolete notifications table if it was created in a previous database run.
     * Notifications are now maintained exclusively as temporary in-memory state.
     */
    private void removeObsoleteNotificationTable() {
        if (jdbcTemplate == null) {
            return;
        }
        try {
            jdbcTemplate.execute("DROP TABLE IF EXISTS notifications");
            log.info("Obsolete notifications database table cleaned up successfully.");
        } catch (Exception e) {
            log.warn("Notice during cleanup of obsolete notifications table: {}", e.getMessage());
        }
    }

    /**
     * Provisions the initial administrator account if configured via environment/properties.
     * Never overwrites existing admin accounts and never falls back to a hardcoded default password.
     */
    private void provisionInitialAdmin() {
        boolean adminAlreadyExists = userRepository.existsByRole(Role.ROLE_ADMIN)
                || (adminUsername != null && !adminUsername.trim().isEmpty() && userRepository.existsByUsername(adminUsername.trim()))
                || (adminEmail != null && !adminEmail.trim().isEmpty() && userRepository.existsByEmail(adminEmail.trim()));

        if (adminAlreadyExists) {
            log.info("Administrator account already exists in database. Skipping initial admin provisioning.");
            return;
        }

        if (adminPassword == null || adminPassword.trim().isEmpty()) {
            log.warn("==========================================================================================");
            log.warn("INITIAL ADMIN PROVISIONING NOTICE:");
            log.warn("Property 'nutricheck.admin.password' (or environment variable 'NUTRICHECK_ADMIN_PASSWORD') is unset or empty.");
            log.warn("No administrator account was provisioned. In production or local setup, please configure:");
            log.warn("  - NUTRICHECK_ADMIN_USERNAME (e.g. admin)");
            log.warn("  - NUTRICHECK_ADMIN_EMAIL (e.g. admin@nutricheck.com)");
            log.warn("  - NUTRICHECK_ADMIN_PASSWORD (required)");
            log.warn("==========================================================================================");
            return;
        }

        if (adminUsername == null || adminUsername.trim().isEmpty() || adminEmail == null || adminEmail.trim().isEmpty()) {
            log.warn("Initial admin provisioning skipped: 'nutricheck.admin.username' and 'nutricheck.admin.email' must be configured and non-empty.");
            return;
        }

        String uname = adminUsername.trim();
        String uemail = adminEmail.trim();
        String uFullName = (adminFullName != null && !adminFullName.trim().isEmpty()) ? adminFullName.trim() : "System Administrator";
        String uPhone = (adminPhone != null && !adminPhone.trim().isEmpty()) ? adminPhone.trim() : "+1 555-0199";

        User admin = new User(
                uname,
                uemail,
                passwordEncoder.encode(adminPassword.trim()),
                uFullName,
                uPhone,
                Role.ROLE_ADMIN,
                38,
                "Other"
        );
        userRepository.save(admin);
        log.info("Initial administrator account successfully provisioned (Username: '{}', Email: '{}').", uname, uemail);
    }

    /**
     * Seeds sample mock users when enabled (e.g., during integration test suites).
     */
    private void seedSampleData() {
        if (!userRepository.existsByUsername("john_doe")) {
            User patientJohn = new User(
                    "john_doe",
                    "john@example.com",
                    passwordEncoder.encode("patient123"),
                    "John Doe",
                    "+1 555-0101",
                    Role.ROLE_PATIENT,
                    30,
                    "Male"
            );
            userRepository.save(patientJohn);
        }

        if (!userRepository.existsByUsername("emily_clark")) {
            User patientEmily = new User(
                    "emily_clark",
                    "emily@example.com",
                    passwordEncoder.encode("patient123"),
                    "Emily Clark",
                    "+1 555-0102",
                    Role.ROLE_PATIENT,
                    28,
                    "Female"
            );
            userRepository.save(patientEmily);
        }

        if (!userRepository.existsByUsername("dr_sarah")) {
            User drSarah = new User(
                    "dr_sarah",
                    "sarah@nutricheck.com",
                    passwordEncoder.encode("nutritionist123"),
                    "Dr. Sarah Jenkins",
                    "+1 555-0103",
                    Role.ROLE_NUTRITIONIST,
                    35,
                    "Female",
                    "RD-12345",
                    "Clinical Nutrition"
            );
            userRepository.save(drSarah);
        }
        log.info("Sample test/demo data successfully seeded.");
    }

}
