package com.example.demo.dtos.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StaffSearchRequest {

    @Size(max = 100)
    private String keyword;
}

