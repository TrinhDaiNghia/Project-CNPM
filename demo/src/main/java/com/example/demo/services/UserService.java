package com.example.demo.services;

import com.example.demo.entities.User;
import com.example.demo.entities.enums.UserRole;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessControlService accessControlService;

    public User createUser(User user) {
        accessControlService.requireOwnerRole();
        if (user.getRole() == null) {
            throw new IllegalArgumentException("Role is required");
        }
        if (user.getRole() == UserRole.CUSTOMER) {
            throw new IllegalStateException("Use /api/auth/register to create CUSTOMER accounts");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalStateException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalStateException("Email already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User updateUser(String id, User user) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        User currentUser = accessControlService.getCurrentUserOrThrow();
        boolean isOwner = accessControlService.isOwner(currentUser);
        boolean isSelf = currentUser.getId().equals(existing.getId());

        if (!isOwner && !isSelf) {
            throw new org.springframework.security.access.AccessDeniedException("You are not allowed to update this user");
        }

        if (!existing.getUsername().equals(user.getUsername()) && userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalStateException("Username already exists");
        }
        if (!existing.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalStateException("Email already exists");
        }

        // Password updates are intentionally handled by auth flow (OTP/reset-password).
        existing.setUsername(user.getUsername());
        existing.setFullName(user.getFullName());
        existing.setEmail(user.getEmail());
        existing.setPhone(user.getPhone());
        existing.setAddress(user.getAddress());
        existing.setGender(user.getGender());
        if (isOwner) {
            if (user.getRole() != null) {
                existing.setRole(user.getRole());
            }
        } else if (user.getRole() != null && user.getRole() != existing.getRole()) {
            throw new org.springframework.security.access.AccessDeniedException("Only OWNER can change user role");
        }

        return userRepository.save(existing);
    }

    public void deleteUser(String id) {
        accessControlService.requireOwnerRole();
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(String id) {
        accessControlService.requireUserSelfOrPrivileged(id);
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        accessControlService.requirePrivilegedRole();
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        accessControlService.requirePrivilegedRole();
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public List<User> findAllByRole(UserRole role) {
        accessControlService.requirePrivilegedRole();
        return userRepository.findByRole(role);
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        accessControlService.requirePrivilegedRole();
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        accessControlService.requirePrivilegedRole();
        return userRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        accessControlService.requirePrivilegedRole();
        return userRepository.existsByEmail(email);
    }
}





