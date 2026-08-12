package com.shiptrack.shiptrack_pro.dto;
 
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
 
@Data
public class RoleUpdateRequest {
 
    @NotBlank(message = "Role is required")
    private String role;
}
