package com.shiptrack.shiptrack_pro.config;
 
import com.shiptrack.shiptrack_pro.security.JwtAuthFilter;
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
@EnableMethodSecurity   // activates @PreAuthorize on controller methods
@RequiredArgsConstructor
public class SecurityConfig {
 
    private final JwtAuthFilter jwtAuthFilter;
 
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
 
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/auth/**").permitAll()
 
                    .requestMatchers(HttpMethod.POST, "/api/shipments")
                            .hasAnyRole("CUSTOMER", "BUSINESS_CLIENT")
 
                    .requestMatchers("/api/tracking/**", "/api/routes/**")
                            .hasAnyRole("LOGISTICS_OPERATOR", "ADMINISTRATOR")
 
                    .requestMatchers(HttpMethod.POST, "/api/pod/**")
                            .hasRole("LOGISTICS_OPERATOR")
 
                    .requestMatchers("/api/analytics/**", "/api/reports/**")
                            .hasAnyRole("BUSINESS_CLIENT", "ADMINISTRATOR")
 
                    .requestMatchers("/api/admin/**").hasRole("ADMINISTRATOR")
 
                    .anyRequest().authenticated()
            )
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
 
        return http.build();
    }
}
