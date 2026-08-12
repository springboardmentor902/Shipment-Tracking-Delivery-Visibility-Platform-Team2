package com.shiptrack.shiptrack_pro.dto;
 
import lombok.*;
 
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String tokenType;
    private UserResponse user;
}
