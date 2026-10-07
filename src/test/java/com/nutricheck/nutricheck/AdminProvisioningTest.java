package com.nutricheck.nutricheck;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricheck.nutricheck.config.DataInitializer;
import com.nutricheck.nutricheck.model.Role;
import com.nutricheck.nutricheck.model.User;
import com.nutricheck.nutricheck.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AdminProvisioningTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DataInitializer dataInitializer;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Admin provisioning creates admin account with hashed password from configuration")
    public void testAdminProvisionedProperly() {
        Optional<User> adminOpt = userRepository.findByUsername("admin");
        assertTrue(adminOpt.isPresent(), "Configured admin user must be provisioned");

        User admin = adminOpt.get();
        assertEquals(Role.ROLE_ADMIN, admin.getRole());
        assertEquals("admin@nutricheck.com", admin.getEmail());
        assertTrue(passwordEncoder.matches("admin123", admin.getPassword()), "Admin password must match configured hash");
    }

    @Test
    @DisplayName("DataInitializer is strictly idempotent and does not overwrite admin or reset password on re-run")
    public void testInitializerIdempotency() throws Exception {
        User admin = userRepository.findByUsername("admin").orElseThrow();
        String originalPasswordHash = admin.getPassword();
        long userCountBefore = userRepository.count();

        // Run dataInitializer again to simulate application restart
        dataInitializer.run();

        long userCountAfter = userRepository.count();
        assertEquals(userCountBefore, userCountAfter, "Re-running initializer must not duplicate accounts");

        User adminAfter = userRepository.findByUsername("admin").orElseThrow();
        assertEquals(originalPasswordHash, adminAfter.getPassword(), "Admin password hash must remain unchanged");
    }

    @Test
    @DisplayName("User entity Jackson serialization does not expose password or hash")
    public void testUserJsonSerializationOmitsPassword() throws Exception {
        User admin = userRepository.findByUsername("admin").orElseThrow();

        String json = objectMapper.writeValueAsString(admin);

        assertFalse(json.contains("admin123"), "JSON must never contain plain password");
        assertFalse(json.contains("password"), "JSON serialization must omit password field due to WRITE_ONLY access");
    }

    @Test
    @DisplayName("User toString does not print password")
    public void testUserToStringOmitsPassword() {
        User admin = userRepository.findByUsername("admin").orElseThrow();
        String str = admin.toString();

        assertFalse(str.contains(admin.getPassword()), "toString must never print password hash");
        assertFalse(str.contains("admin123"), "toString must never print plain password");
    }

    @Test
    @DisplayName("Unconfigured admin password safely skips admin creation without using hardcoded defaults")
    public void testUnconfiguredAdminPasswordSkipsProvisioning() {
        // Create an isolated DataInitializer with no password configured
        UserRepository mockUserRepo = org.mockito.Mockito.mock(UserRepository.class);

        org.mockito.Mockito.when(mockUserRepo.existsByRole(Role.ROLE_ADMIN)).thenReturn(false);
        org.mockito.Mockito.when(mockUserRepo.existsByUsername("new_admin")).thenReturn(false);

        DataInitializer isolatedInitializer = new DataInitializer(
                mockUserRepo, passwordEncoder
        );

        // Intentionally run without setting admin password (simulating empty / unconfigured property)
        assertDoesNotThrow(() -> isolatedInitializer.run());

        // Verify that save was never called for an unconfigured admin
        org.mockito.Mockito.verify(mockUserRepo, org.mockito.Mockito.never()).save(
                org.mockito.Mockito.argThat(u -> u != null && u.getRole() == Role.ROLE_ADMIN)
        );
    }
}
