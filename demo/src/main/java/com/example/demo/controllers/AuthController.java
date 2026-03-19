package com.example.demo.controllers;

import com.example.demo.dtos.request.ForgotPasswordRequest;
import com.example.demo.dtos.request.LoginRequest;
import com.example.demo.dtos.request.RegisterRequest;
import com.example.demo.dtos.request.ResetPasswordRequest;
import com.example.demo.dtos.request.VerifyEmailOtpRequest;
import com.example.demo.dtos.response.OtpResponse;
import com.example.demo.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<OtpResponse> register(@Valid @RequestBody RegisterRequest request) {
        OtpResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody VerifyEmailOtpRequest request) {
        return authService.verifyRegisterOtp(request)
                .<ResponseEntity<?>>map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .orElseGet(() -> ResponseEntity.badRequest()
                        .body(Map.of("message", "Invalid or expired OTP")));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid username/email or password")));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<OtpResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        OtpResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        boolean resetSuccess = authService.resetPassword(request);
        if (!resetSuccess) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid email or OTP"));
        }
        return ResponseEntity.ok(Map.of("message", "Password reset successful"));
    }
}
