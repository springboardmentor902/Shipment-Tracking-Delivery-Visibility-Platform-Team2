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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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
                .cors(cors -> {})

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

                        // Shipment creation is restricted by role; the service
                        // records the current user as the owner.
                        .requestMatchers(HttpMethod.POST, "/api/shipments")
                        .hasAnyRole("CUSTOMER", "BUSINESS_CLIENT")

                        .requestMatchers(HttpMethod.PUT, "/api/shipments/**")
                        .authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/shipments/**")
                        .authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/shipments/**")
                        .authenticated()

                        // Route changes and tracking updates are operational
                        // actions. Read access is verified at entity level.
                        .requestMatchers(HttpMethod.POST, "/api/routes/**",
                                "/api/tracking/**", "/api/eta/**")
                        .hasAnyRole("LOGISTICS_OPERATOR", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.PUT, "/api/routes/**")
                        .hasAnyRole("LOGISTICS_OPERATOR", "ADMINISTRATOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/routes/**")
                        .hasAnyRole("LOGISTICS_OPERATOR", "ADMINISTRATOR")

                        // Proof of delivery
                        .requestMatchers(HttpMethod.POST, "/api/pod/**")
                        .hasRole("LOGISTICS_OPERATOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/pod/**")
                        .hasAnyRole("SUPPORT_AGENT", "ADMINISTRATOR")

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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
