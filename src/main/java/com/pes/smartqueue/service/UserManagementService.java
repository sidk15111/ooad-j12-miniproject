package com.pes.smartqueue.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserManagementService {
    private static final List<String> ALLOWED_ROLES = List.of("CUSTOMER", "RECEPTIONIST", "SERVICE_STAFF", "ADMIN");
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final Map<String, ManagedUserProfile> users = new LinkedHashMap<>();

    public UserManagementService() {
        users.put("admin", new ManagedUserProfile("admin", PASSWORD_ENCODER.encode("admin123"), "ADMIN", true));
        users.put("reception", new ManagedUserProfile("reception", PASSWORD_ENCODER.encode("reception123"), "RECEPTIONIST", true));
        users.put("customer", new ManagedUserProfile("customer", PASSWORD_ENCODER.encode("customer123"), "CUSTOMER", true));
        users.put("staff", new ManagedUserProfile("staff", PASSWORD_ENCODER.encode("staff123"), "SERVICE_STAFF", true));
    }

    public List<ManagedUserProfile> listUsers() {
        return new ArrayList<>(users.values());
    }

    public List<String> allowedRoles() {
        return ALLOWED_ROLES;
    }

    public void approve(String username) {
        ManagedUserProfile profile = require(username);
        profile.setActive(true);
    }

    public void reactivate(String username) {
        ManagedUserProfile profile = require(username);
        profile.setActive(true);
    }

    public void deactivate(String username) {
        ManagedUserProfile profile = require(username);
        profile.setActive(false);
    }

    public void addUser(String username, String password, String role) {
        String normalizedRole = normalizeRole(role);
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (users.containsKey(username)) {
            throw new IllegalArgumentException("User already exists: " + username);
        }
        users.put(username, new ManagedUserProfile(username, PASSWORD_ENCODER.encode(password), normalizedRole, true));
    }

    public void registerCustomer(String username, String password) {
        addUser(username, password, "CUSTOMER");
    }

    public void deleteUser(String username) {
        ManagedUserProfile profile = require(username);
        if ("admin".equalsIgnoreCase(profile.getUsername())) {
            throw new IllegalArgumentException("Default admin account cannot be deleted");
        }
        users.remove(username);
    }

    public boolean isUserActive(String username) {
        ManagedUserProfile profile = users.get(username);
        if (profile == null) {
            return false;
        }
        return profile.isActive();
    }

    public ManagedUserProfile findByUsername(String username) {
        return users.get(username);
    }

    public void resetSeedUsersToActive() {
        users.values().forEach(profile -> profile.setActive(true));
    }

    private ManagedUserProfile require(String username) {
        ManagedUserProfile profile = users.get(username);
        if (profile == null) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        return profile;
    }

    private String normalizeRole(String role) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }
        String normalized = role.trim().toUpperCase();
        if (!ALLOWED_ROLES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported role. Allowed roles: " + Arrays.toString(ALLOWED_ROLES.toArray()));
        }
        return normalized;
    }

    public static class ManagedUserProfile {
        private final String username;
        private final String password;
        private final String role;
        private boolean active;

        public ManagedUserProfile(String username, String password, String role, boolean active) {
            this.username = username;
            this.password = password;
            this.role = role;
            this.active = active;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getRole() {
            return role;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }
}