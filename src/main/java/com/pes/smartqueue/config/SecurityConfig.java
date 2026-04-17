package com.pes.smartqueue.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/css/**").authenticated()
                .requestMatchers("/reception/**").hasAnyRole("RECEPTIONIST", "ADMIN")
                .requestMatchers("/customer/**").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers("/staff/**").hasAnyRole("SERVICE_STAFF", "ADMIN")
                .anyRequest().authenticated())
            .formLogin(Customizer.withDefaults())
            .logout(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails receptionist = User.withUsername("reception")
            .password(passwordEncoder.encode("reception123"))
            .roles("RECEPTIONIST")
            .build();

        UserDetails admin = User.withUsername("admin")
            .password(passwordEncoder.encode("admin123"))
            .roles("ADMIN")
            .build();

        UserDetails customer = User.withUsername("customer")
            .password(passwordEncoder.encode("customer123"))
            .roles("CUSTOMER")
            .build();

        UserDetails staff = User.withUsername("staff")
            .password(passwordEncoder.encode("staff123"))
            .roles("SERVICE_STAFF")
            .build();

        return new InMemoryUserDetailsManager(receptionist, admin, customer, staff);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
