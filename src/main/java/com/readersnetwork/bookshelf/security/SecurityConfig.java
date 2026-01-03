package com.readersnetwork.bookshelf.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for REST API
                .csrf(csrf -> csrf.disable())

                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - no authentication required
                        .requestMatchers(
                                "/api/auth/**", // All auth endpoints (login, register, check-email, etc.)
                                "/api/users/register", // Legacy register endpoint if still needed
                                "/api/books/**", // All book endpoints
                                "/h2-console/**", // H2 database console
                                "/actuator/**", // Actuator endpoints
                                "/error" // Error page
                        ).permitAll()

                        // All other endpoints require authentication
                        .anyRequest().authenticated())

                // Stateless session management (for JWT)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Allow H2 console frames (for development)
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}