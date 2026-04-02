package com.example.demo.dtos.response;

import com.example.demo.entities.enums.UserGender;
import com.example.demo.entities.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class CustomerResponse {

    private String id;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private UserGender gender;
    private UserRole role;
    private Boolean isActive;
    private Date createdAt;
}

