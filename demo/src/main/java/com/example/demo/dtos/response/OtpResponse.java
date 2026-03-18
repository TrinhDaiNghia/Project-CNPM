package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OtpResponse {

    private String message;
    private String email;
    private long expiresInSeconds;
}
