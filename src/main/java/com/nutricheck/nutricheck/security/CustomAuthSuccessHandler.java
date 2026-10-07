package com.nutricheck.nutricheck.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class CustomAuthSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();
            if (role.equals("ROLE_ADMIN")) {
                response.sendRedirect("/admin/dashboard");
                return;
            } else if (role.equals("ROLE_HEALTHCARE_PROFESSIONAL")) {
                response.sendRedirect("/healthcare-professional/dashboard");
                return;
            } else if (role.equals("ROLE_NUTRITIONIST")) {
                response.sendRedirect("/nutritionist/dashboard");
                return;
            } else if (role.equals("ROLE_PATIENT")) {
                response.sendRedirect("/patient/dashboard");
                return;
            }
        }

        response.sendRedirect("/");
    }
}
