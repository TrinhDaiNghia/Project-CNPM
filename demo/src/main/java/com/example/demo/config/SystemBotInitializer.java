package com.example.demo.config;

import com.example.demo.entities.User;
import com.example.demo.entities.enums.UserRole;
import com.example.demo.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class SystemBotInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${notification.system-bot.username:system.bot}")
    private String botUsername;

    @Value("${notification.system-bot.email:system.bot@local}")
    private String botEmail;

    @Value("${notification.system-bot.password:system-bot-secret}")
    private String botPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String normalizedUsername = normalize(botUsername);
        String normalizedEmail = normalize(botEmail).toLowerCase();

        if (!StringUtils.hasText(normalizedUsername) || !StringUtils.hasText(normalizedEmail)) {
            throw new IllegalStateException("notification.system-bot.username and notification.system-bot.email are required");
        }

        User byUsername = userRepository.findByUsername(normalizedUsername).orElse(null);
        User byEmail = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (byUsername != null && byEmail != null && !byUsername.getId().equals(byEmail.getId())) {
            throw new IllegalStateException("System BOT config is ambiguous: username and email point to different users");
        }

        User bot = byUsername != null ? byUsername : byEmail;
        if (bot == null) {
            User newBot = User.builder()
                    .username(normalizedUsername)
                    .password(passwordEncoder.encode(normalize(botPassword)))
                    .email(normalizedEmail)
                    .address("SYSTEM")
                    .role(UserRole.STAFF)
                    .build();
            userRepository.save(newBot);
            return;
        }

        boolean changed = false;
        if (!StringUtils.hasText(bot.getAddress())) {
            bot.setAddress("SYSTEM");
            changed = true;
        }
        if (bot.getRole() != UserRole.STAFF && bot.getRole() != UserRole.OWNER) {
            bot.setRole(UserRole.STAFF);
            changed = true;
        }

        if (changed) {
            userRepository.save(bot);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
