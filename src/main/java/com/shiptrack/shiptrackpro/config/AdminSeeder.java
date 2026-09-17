package com.shiptrack.shiptrackpro.config;

import com.shiptrack.shiptrackpro.entity.User;
import com.shiptrack.shiptrackpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL:admin@example.com}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Override
    public void run(String... args) {

        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            return;
        }

        var existingAdmin = userRepository.findByEmailIgnoreCase(adminEmail);
        if (existingAdmin.isPresent()) {
            User admin = existingAdmin.get();
            boolean changed = false;
            if (!"ADMINISTRATOR".equals(admin.getRole())) {
                admin.setRole("ADMINISTRATOR");
                changed = true;
            }
            if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode(adminPassword));
                changed = true;
            }
            if (changed) {
                userRepository.save(admin);
            }
            return;
        }

        User admin = User.builder()
                .fullName("System Administrator")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .phone("0000000000")
                .role("ADMINISTRATOR")
                .status("ACTIVE")
                .build();

        userRepository.save(admin);

        System.out.println("Seeded administrator account: " + adminEmail);
    }
}
