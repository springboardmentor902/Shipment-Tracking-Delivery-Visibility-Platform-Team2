package com.shiptrack.shiptrackpro.config;

import com.shiptrack.shiptrackpro.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // Public authentication endpoints
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/register"
                        ).permitAll()

                        // Shipment creation
                        .requestMatchers(HttpMethod.POST, "/api/shipments")
                        .hasAnyRole("CUSTOMER", "BUSINESS_CLIENT")

                        // Tracking and routes
                        .requestMatchers("/api/tracking/**", "/api/routes/**")
                        .hasAnyRole("LOGISTICS_OPERATOR", "ADMINISTRATOR")

                        // Proof of delivery
                        .requestMatchers(HttpMethod.POST, "/api/pod/**")
                        .hasRole("LOGISTICS_OPERATOR")

                        // Analytics and reports
                        .requestMatchers("/api/analytics/**", "/api/reports/**")
                        .hasAnyRole("BUSINESS_CLIENT", "ADMINISTRATOR")

                        // Admin
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMINISTRATOR")

                        // Everything else requires authentication
                        .anyRequest()
                        .authenticated()
                )

                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())

                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}