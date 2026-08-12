package com.shiptrack.shiptrack_pro.entity;
 
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
 
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(name = "full_name", nullable = false)
    private String fullName;
 
    @Column(name = "email", nullable = false, unique = true)
    private String email;
 
    @Column(name = "password", nullable = false)
    private String password;
 
    private String phone;
    private String role;
    private String status;
 
    private String profileImageUrl;
 
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
 
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
 
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
