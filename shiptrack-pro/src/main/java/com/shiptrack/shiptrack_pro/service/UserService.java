package com.shiptrack.shiptrack_pro.service;
 
import com.shiptrack.shiptrack_pro.dto.LoginRequest;
import com.shiptrack.shiptrack_pro.dto.LoginResponse;
import com.shiptrack.shiptrack_pro.dto.RegisterRequest;
import com.shiptrack.shiptrack_pro.dto.UserResponse;
 
import java.util.List;
 
public interface UserService {
    UserResponse registerUser(RegisterRequest request);
    LoginResponse loginUser(LoginRequest request);
    List<UserResponse> getAllUsers();
    UserResponse updateUserRole(Long userId, String newRole);
}
