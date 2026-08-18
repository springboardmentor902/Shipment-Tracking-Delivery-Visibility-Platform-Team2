package com.shiptrack.shiptrackpro.service.impl;

import com.shiptrack.shiptrackpro.dto.LoginRequest;
import com.shiptrack.shiptrackpro.dto.LoginResponse;
import com.shiptrack.shiptrackpro.dto.RegisterRequest;
import com.shiptrack.shiptrackpro.dto.UserResponse;
import com.shiptrack.shiptrackpro.entity.User;
import com.shiptrack.shiptrackpro.repository.UserRepository;
import com.shiptrack.shiptrackpro.security.Role;
import com.shiptrack.shiptrackpro.security.JwtUtil;
import com.shiptrack.shiptrackpro.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public UserResponse registerUser(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already registered"
            );
        }

        Role requestedRole;

        try {
            requestedRole =
                    Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid role: " + request.getRole()
                            + ". Must be one of: "
                            + Arrays.toString(Role.values())
            );
        }

        if (requestedRole == Role.ADMINISTRATOR) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Administrator accounts cannot be created through registration."
            );
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(requestedRole.name())
                .status("ACTIVE")
                .build();

        User savedUser = userRepository.save(user);

        return mapToResponse(savedUser);
    }

    @Override
    public LoginResponse loginUser(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"
                ));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Account is not active. Current status: "
                            + user.getStatus()
            );
        }

        user.setLastLoginAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);

        String token = jwtUtil.generateToken(
                updatedUser.getEmail(),
                updatedUser.getRole()
        );

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(mapToResponse(updatedUser))
                .build();
    }

    @Override
    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public UserResponse updateUserRole(
            Long userId,
            String newRole) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found with id: " + userId
                ));

        Role role;

        try {
            role = Role.valueOf(newRole.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid role: " + newRole
                            + ". Must be one of: "
                            + Arrays.toString(Role.values())
            );
        }

        if (role == Role.ADMINISTRATOR
                && userRepository.existsByRole("ADMINISTRATOR")) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An administrator account already exists. Only one administrator is allowed."
            );
        }

        user.setRole(role.name());

        User updatedUser = userRepository.save(user);

        return mapToResponse(updatedUser);
    }

    private UserResponse mapToResponse(User user) {

        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}