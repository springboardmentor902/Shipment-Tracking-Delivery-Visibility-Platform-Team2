package com.shiptrack.shiptrack_pro.config;
 
import com.shiptrack.shiptrack_pro.entity.User;
import com.shiptrack.shiptrack_pro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
 
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {
 
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
 
    @Override
    public void run(String... args) {
        String adminEmail = "admin@shiptrack.com";
 
        if (userRepository.existsByEmail(adminEmail)) {
            return; // already seeded — do nothing on subsequent restarts
        }
 
        User admin = User.builder()
                .fullName("System Administrator")
                .email(adminEmail)
                .password(passwordEncoder.encode("Admin@123"))
                .phone("0000000000")
                .role("ADMINISTRATOR")
                .status("ACTIVE")
                .build();
 
        userRepository.save(admin);
        System.out.println("Seeded default admin account: " + adminEmail);
    }
}
