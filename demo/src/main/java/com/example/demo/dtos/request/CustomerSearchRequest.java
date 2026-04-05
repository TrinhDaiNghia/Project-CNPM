package com.example.demo.dtos.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerSearchRequest {

    @Size(max = 80)
    private String fullName;

    @Size(max = 100)
    private String email;

    @Size(max = 20)
    private String phone;

    @Size(max = 255)
    private String address;
}

