package com.shiptrack.shiptrack_pro.dto;
 
import jakarta.validation.constraints.*;
import lombok.Data;
 
@Data
public class LoginRequest {
 
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
 
    @NotBlank(message = "Password is required")
    private String password;
}
