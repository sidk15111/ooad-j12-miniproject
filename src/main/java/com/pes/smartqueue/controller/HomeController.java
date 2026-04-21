package com.pes.smartqueue.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication == null) {
            return "home";
        }
        if (hasRole(authentication, "ROLE_ADMIN")) {
            return "redirect:/admin/dashboard";
        }
        if (hasRole(authentication, "ROLE_RECEPTIONIST")) {
            return "redirect:/reception/queue";
        }
        if (hasRole(authentication, "ROLE_CUSTOMER")) {
            return "redirect:/customer/appointments";
        }
        if (hasRole(authentication, "ROLE_SERVICE_STAFF")) {
            return "redirect:/staff/sessions";
        }
        return "home";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }

    private boolean hasRole(Authentication authentication, String role) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (role.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
