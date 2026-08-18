package com.shiptrack.shiptrackpro.controller;

import com.shiptrack.shiptrackpro.dto.LoginRequest;
import com.shiptrack.shiptrackpro.dto.LoginResponse;
import com.shiptrack.shiptrackpro.dto.RegisterRequest;
import com.shiptrack.shiptrackpro.dto.UserResponse;
import com.shiptrack.shiptrackpro.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                userService.loginUser(request)
        );
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.registerUser(request));
    }
}