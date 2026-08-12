package com.shiptrack.shiptrack_pro.repository;
 
import com.shiptrack.shiptrack_pro.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
 
import java.util.Optional;
 
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByRole(String role);
}
