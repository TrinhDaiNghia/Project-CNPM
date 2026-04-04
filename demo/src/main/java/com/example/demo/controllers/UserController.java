package com.example.demo.controllers;

import com.example.demo.dtos.request.ChangePasswordRequest;
import com.example.demo.dtos.request.UserCreateRequest;
import com.example.demo.dtos.request.UserUpdateRequest;
import com.example.demo.dtos.response.UserResponse;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.UserRole;
import com.example.demo.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll() {
        List<UserResponse> responses = userService.findAll().stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable String id) {
        return userService.findById(id)
                .map(this::toUserResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserResponse>> getByRole(@PathVariable UserRole role) {
        List<UserResponse> responses = userService.findAllByRole(role).stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        User created = userService.createUser(toUserEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(toUserResponse(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable String id, @Valid @RequestBody UserUpdateRequest request) {
        User updated = userService.updateUser(id, toUserEntity(request));
        return ResponseEntity.ok(toUserResponse(updated));
    }

    @PatchMapping("/{id}/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @PathVariable String id,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(id, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/lock")
    public ResponseEntity<UserResponse> lockStaff(@PathVariable String id) {
        User updated = userService.lockStaff(id);
        return ResponseEntity.ok(toUserResponse(updated));
    }

    @PatchMapping("/{id}/unlock")
    public ResponseEntity<UserResponse> unlockStaff(@PathVariable String id) {
        User updated = userService.unlockStaff(id);
        return ResponseEntity.ok(toUserResponse(updated));
    }

    private User toUserEntity(UserCreateRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setGender(request.getGender());
        user.setRole(request.getRole());
        return user;
    }

    private User toUserEntity(UserUpdateRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setGender(request.getGender());
        user.setRole(request.getRole());
        return user;
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .gender(user.getGender())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
