package com.example.demo.dtos.request;

import com.example.demo.entities.enums.WarrantyStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WarrantySearchRequest {

    @Size(max = 100)
    private String keyword;

    private WarrantyStatus status;
}

