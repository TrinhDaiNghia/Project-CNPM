package com.example.demo.dtos.request;

import com.example.demo.entities.enums.UserGender;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^(\\+84|0)[3-9]\\d{8}$", message = "Invalid Vietnamese phone number")
    private String phone;

    @Size(max = 255)
    private String address;

    private UserGender gender;
}
