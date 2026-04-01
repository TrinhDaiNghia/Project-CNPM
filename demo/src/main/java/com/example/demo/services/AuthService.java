package com.example.demo.services;

import com.example.demo.dtos.request.ForgotPasswordRequest;
import com.example.demo.dtos.request.LoginRequest;
import com.example.demo.dtos.request.RegisterRequest;
import com.example.demo.dtos.request.ResetPasswordRequest;
import com.example.demo.dtos.request.VerifyEmailOtpRequest;
import com.example.demo.dtos.response.AuthResponse;
import com.example.demo.dtos.response.OtpResponse;
import com.example.demo.entities.User;
import com.example.demo.entities.Customer;
import com.example.demo.entities.enums.UserRole;
import com.example.demo.repositories.UserRepository;
import com.example.demo.repositories.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailOtpService emailOtpService;
    private final JwtService jwtService;
    private final UserProfileService userProfileService;
    private final NotificationService notificationService;
    private final CustomerRepository customerRepository;
    private final Map<String, PendingRegistration> pendingRegistrations = new ConcurrentHashMap<>();

    public OtpResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email already exists");
        }
         
        RegisterRequest pendingRequest = copyRegisterRequest(request);
        pendingRequest.setPassword(passwordEncoder.encode(request.getPassword()));

        long expiresInSeconds = emailOtpService.sendRegistrationOtp(request.getEmail());
        String emailKey = normalizeEmail(request.getEmail());
        pendingRegistrations.put(emailKey,
            new PendingRegistration(pendingRequest, Instant.now().plusSeconds(expiresInSeconds)));

        return OtpResponse.builder()
                .message("OTP has been sent to email. Please verify to complete registration")
                .email(request.getEmail())
                .expiresInSeconds(expiresInSeconds)
                .build();
    }

    public Optional<AuthResponse> verifyRegisterOtp(VerifyEmailOtpRequest request) {
        String emailKey = normalizeEmail(request.getEmail());
        PendingRegistration pending = pendingRegistrations.get(emailKey);
        if (pending == null) {
            return Optional.empty();
        }

        if (Instant.now().isAfter(pending.getExpiresAt())) {
            pendingRegistrations.remove(emailKey);
            return Optional.empty();
        }

        boolean validOtp = emailOtpService.verifyRegistrationOtp(request.getEmail(), request.getOtp());
        if (!validOtp) {
            return Optional.empty();
        }

        RegisterRequest registerRequest = pending.getRegisterRequest();
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            pendingRegistrations.remove(emailKey);
            throw new IllegalStateException("Email already exists");
        }
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            pendingRegistrations.remove(emailKey);
            throw new IllegalStateException("Username already exists");
        }

        Customer customer = new Customer();
        customer.setUsername(registerRequest.getUsername());
        customer.setPassword(registerRequest.getPassword());
        customer.setFullName(registerRequest.getFullName());
        customer.setEmail(registerRequest.getEmail());
        customer.setPhone(registerRequest.getPhone());
        customer.setAddress(registerRequest.getAddress());
        customer.setGender(registerRequest.getGender());
        customer.setRole(UserRole.CUSTOMER);

        User savedUser = userRepository.save(Objects.requireNonNull(customer));
        pendingRegistrations.remove(emailKey);
        notificationService.sendRegistrationSuccessNotification(savedUser);

        return Optional.of(buildAuthResponse(savedUser, "Register successful"));
    }

    @Transactional(readOnly = true)
    public Optional<AuthResponse> login(LoginRequest request) {
        String identifier = normalizeLoginIdentifier(request.getUsernameOrEmail());
        Optional<User> optionalUser = userRepository.findByUsername(identifier);
        if (optionalUser.isEmpty()) {
            optionalUser = userRepository.findByEmail(normalizeEmail(identifier));
        }
        if (optionalUser.isEmpty()) {
            return Optional.empty();
        }

        User user = optionalUser.get();
        boolean validPassword = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!validPassword) {
            return Optional.empty();
        }

        return Optional.of(buildAuthResponse(user, "Login successful"));
    }
    public OtpResponse forgotPassword(ForgotPasswordRequest request) {
        long expiresInSeconds = 600;
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());
        if (optionalUser.isPresent()) {
            expiresInSeconds = emailOtpService.sendPasswordResetOtp(request.getEmail());
        }

        return OtpResponse.builder()
                .message("If the email exists, an OTP has been sent")
                .email(request.getEmail())
                .expiresInSeconds(expiresInSeconds)
                .build();
    }
    public boolean resetPassword(ResetPasswordRequest request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());
        if (optionalUser.isEmpty()) {
            return false;
        }

        boolean validOtp = emailOtpService.verifyPasswordResetOtp(request.getEmail(), request.getOtp());
        if (!validOtp) {
            return false;
        }

        User user = optionalUser.get();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return true;
    }

    private AuthResponse buildAuthResponse(User user, String message) {
        String accessToken = jwtService.generateToken(user);
        return AuthResponse.builder()
                .message(message)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
            .tokenType("Bearer")
            .accessToken(accessToken)
            .expiresInSeconds(jwtService.getExpirationInSeconds())
                .build();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String normalizeLoginIdentifier(String identifier) {
        return identifier == null ? "" : identifier.trim();
    }

    private RegisterRequest copyRegisterRequest(RegisterRequest request) {
        RegisterRequest copy = new RegisterRequest();
        copy.setUsername(request.getUsername());
        copy.setPassword(request.getPassword());
        copy.setFullName(request.getFullName());
        copy.setEmail(request.getEmail());
        copy.setPhone(request.getPhone());
        copy.setAddress(request.getAddress());
        copy.setGender(request.getGender());
        return copy;
    }

    private static class PendingRegistration {
        private final RegisterRequest registerRequest;
        private final Instant expiresAt;

        private PendingRegistration(RegisterRequest registerRequest, Instant expiresAt) {
            this.registerRequest = registerRequest;
            this.expiresAt = expiresAt;
        }

        private RegisterRequest getRegisterRequest() {
            return registerRequest;
        }

        private Instant getExpiresAt() {
            return expiresAt;
        }
    }
}

