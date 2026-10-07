package com.nutricheck.nutricheck.config;

import com.nutricheck.nutricheck.security.CustomAuthSuccessHandler;
import com.nutricheck.nutricheck.security.CustomUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.UUID;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthSuccessHandler successHandler;

    @Value("${nutricheck.security.remember-me-key:}")
    private String configuredRememberMeKey;

    public SecurityConfig(CustomUserDetailsService userDetailsService, CustomAuthSuccessHandler successHandler) {
        this.userDetailsService = userDetailsService;
        this.successHandler = successHandler;
    }

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        String activeRememberMeKey;
        if (configuredRememberMeKey != null && !configuredRememberMeKey.trim().isEmpty()) {
            activeRememberMeKey = configuredRememberMeKey.trim();
        } else {
            activeRememberMeKey = UUID.randomUUID().toString();
            log.warn("==========================================================================================");
            log.warn("SECURITY NOTICE: 'nutricheck.security.remember-me-key' (or NUTRICHECK_REMEMBER_ME_KEY) is unset.");
            log.warn("An ephemeral random key was generated for this server session. Remember-me tokens will reset on restart.");
            log.warn("To maintain persistent sessions across server restarts in production, configure NUTRICHECK_REMEMBER_ME_KEY.");
            log.warn("==========================================================================================");
        }

        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    new AntPathRequestMatcher("/h2-console/**"),
                    new AntPathRequestMatcher("/bmi-calculator")
                )
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            )
            .authorizeHttpRequests(auth -> auth
                // Static resources
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico", "/h2-console/**").permitAll()
                // Public landing and registration routes
                .requestMatchers("/", "/about", "/bmi-calculator", "/login", "/register").permitAll()
                // Role-based routes
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/healthcare-professional/**").hasAnyRole("HEALTHCARE_PROFESSIONAL", "ADMIN")
                .requestMatchers("/nutritionist/**").hasAnyRole("NUTRITIONIST", "HEALTHCARE_PROFESSIONAL", "ADMIN")
                .requestMatchers("/patient/**").hasAnyRole("PATIENT", "ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(successHandler)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .rememberMe(remember -> remember
                .key(activeRememberMeKey)
                .tokenValiditySeconds(30 * 24 * 60 * 60) // 30 days
                .rememberMeParameter("remember-me")
                .userDetailsService(userDetailsService)
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/access-denied")
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            );

        return http.build();
    }
}
