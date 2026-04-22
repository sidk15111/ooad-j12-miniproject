package com.pes.smartqueue.controller;

import com.pes.smartqueue.service.UserManagementService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {
    private final UserManagementService userManagementService;

    public HomeController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

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

    @PostMapping("/register/customer")
    public String registerCustomer(@RequestParam String username,
                                   @RequestParam String password,
                                   RedirectAttributes redirectAttributes) {
        try {
            userManagementService.registerCustomer(username, password);
            redirectAttributes.addFlashAttribute("success", "Registration successful. Please login.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/login";
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
