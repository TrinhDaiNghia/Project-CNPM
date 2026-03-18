package com.example.demo.dtos.response;

import com.example.demo.entities.enums.UserRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    private String message;
    private String userId;
    private String username;
    private String email;
    private UserRole role;
    private String tokenType;
    private String accessToken;
    private long expiresInSeconds;
}
