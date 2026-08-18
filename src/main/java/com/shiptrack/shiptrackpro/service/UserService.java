package com.shiptrack.shiptrackpro.service;

import com.shiptrack.shiptrackpro.dto.LoginRequest;
import com.shiptrack.shiptrackpro.dto.LoginResponse;
import com.shiptrack.shiptrackpro.dto.RegisterRequest;
import com.shiptrack.shiptrackpro.dto.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse registerUser(RegisterRequest request);

    LoginResponse loginUser(LoginRequest request);

    List<UserResponse> getAllUsers();

    UserResponse updateUserRole(Long userId, String newRole);
}