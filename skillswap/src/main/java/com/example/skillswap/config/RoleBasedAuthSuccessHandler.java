package com.example.skillswap.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class RoleBasedAuthSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        String redirectURL = request.getContextPath();

        // Check if the user has ADMIN role
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            redirectURL += "/admin/dashboard";
        }
        // Redirect standard ROLE_USER to the home page (as requested)
        else if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"))) {
            redirectURL += "/";
        } else {
            // Fallback for any other authenticated role
            redirectURL += "/";
        }

        response.sendRedirect(redirectURL);
    }
}


