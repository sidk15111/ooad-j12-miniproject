package com.pes.smartqueue.config;

import com.pes.smartqueue.service.UserManagementService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    private final ActiveUserFilter activeUserFilter;

    public SecurityConfig(ActiveUserFilter activeUserFilter) {
        this.activeUserFilter = activeUserFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register/customer", "/access-denied", "/css/**").permitAll()
                .requestMatchers("/").authenticated()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/reception/**").hasAnyRole("RECEPTIONIST", "ADMIN")
                .requestMatchers("/customer/**").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers("/staff/**").hasAnyRole("SERVICE_STAFF", "ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex.accessDeniedPage("/access-denied"))
            .addFilterAfter(activeUserFilter, AnonymousAuthenticationFilter.class)
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll())
            .logout(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(UserManagementService userManagementService) {
        return username -> {
            UserManagementService.ManagedUserProfile profile = userManagementService.findByUsername(username);
            if (profile == null) {
                throw new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found: " + username);
            }
            return org.springframework.security.core.userdetails.User.withUsername(profile.getUsername())
                .password(profile.getPassword())
                .roles(profile.getRole())
                .disabled(!profile.isActive())
                .build();
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
