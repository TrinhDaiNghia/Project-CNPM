package com.example.demo.services;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class EmailOtpService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_TTL_SECONDS = 600;
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final String REGISTER_PURPOSE = "REGISTER_EMAIL";
    private static final String RESET_PASSWORD_PURPOSE = "RESET_PASSWORD";

    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, OtpPayload> otpStore = new ConcurrentHashMap<>();

    public EmailOtpService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public long sendRegistrationOtp(String email) {
        return sendOtp(email, REGISTER_PURPOSE, "Email verification OTP");
    }

    public boolean verifyRegistrationOtp(String email, String otp) {
        return verifyOtp(email, otp, REGISTER_PURPOSE);
    }

    public long sendPasswordResetOtp(String email) {
        return sendOtp(email, RESET_PASSWORD_PURPOSE, "Password reset OTP");
    }

    public boolean verifyPasswordResetOtp(String email, String otp) {
        return verifyOtp(email, otp, RESET_PASSWORD_PURPOSE);
    }

    private long sendOtp(String email, String purpose, String subject) {
        String otp = generateNumericOtp();
        Instant expiresAt = Instant.now().plusSeconds(OTP_TTL_SECONDS);

        otpStore.put(buildKey(email, purpose), new OtpPayload(otp, expiresAt, 0));
        sendOtpEmail(email, otp, subject);
        return OTP_TTL_SECONDS;
    }

    private boolean verifyOtp(String email, String otp, String purpose) {
        String key = buildKey(email, purpose);
        OtpPayload payload = otpStore.get(key);

        if (payload == null) {
            return false;
        }

        if (Instant.now().isAfter(payload.getExpiresAt())) {
            otpStore.remove(key);
            return false;
        }

        if (payload.getFailedAttempts() >= MAX_VERIFY_ATTEMPTS) {
            otpStore.remove(key);
            return false;
        }

        if (payload.getOtp().equals(otp)) {
            otpStore.remove(key);
            return true;
        }

        payload.setFailedAttempts(payload.getFailedAttempts() + 1);
        if (payload.getFailedAttempts() >= MAX_VERIFY_ATTEMPTS) {
            otpStore.remove(key);
        }
        return false;
    }

    private String generateNumericOtp() {
        int upperBound = (int) Math.pow(10, OTP_LENGTH);
        int lowerBound = (int) Math.pow(10, OTP_LENGTH - 1);
        int otpValue = secureRandom.nextInt(upperBound - lowerBound) + lowerBound;
        return String.valueOf(otpValue);
    }

    private void sendOtpEmail(String email, String otp, String subject) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        message.setText("Your OTP code is: " + otp + "\nThis code will expire in 10 minutes.");

        try {
            // Log OTP for development/debugging so it can be verified without email delivery
            log.info("Generated OTP for {}: {}", email, otp);

            // Send email asynchronously to avoid blocking the registration request
            new Thread(() -> {
                try {
                    mailSender.send(message);
                    log.info("OTP email sent to {}", email);
                } catch (MailException ex) {
                    log.error("Failed to send OTP email to {}", email, ex);
                }
            }, "otp-email-sender").start();
        } catch (Exception ex) {
            // Ensure that email delivery problems do not break registration flow
            log.error("Failed to schedule OTP email to {}", email, ex);
        }
    }

    private String buildKey(String email, String purpose) {
        return purpose + ":" + email.toLowerCase();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    private static class OtpPayload {
        private String otp;
        private Instant expiresAt;
        private int failedAttempts;
    }
}

