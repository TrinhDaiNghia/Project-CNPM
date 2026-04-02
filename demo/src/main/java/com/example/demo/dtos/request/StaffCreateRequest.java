package com.example.demo.dtos.request;

import com.example.demo.entities.enums.UserGender;
import com.example.demo.entities.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StaffCreateRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 80)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^(\\+84|0)[3-9]\\d{8}$", message = "Invalid Vietnamese phone number")
    private String phone;

    @Size(max = 255)
    private String address;

    private UserGender gender;

    private UserRole role = UserRole.STAFF;
}

