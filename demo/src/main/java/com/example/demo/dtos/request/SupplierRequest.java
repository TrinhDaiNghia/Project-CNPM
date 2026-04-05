package com.example.demo.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupplierRequest {

    @NotBlank(message = "Supplier name is required")
    @Size(max = 100, message = "Supplier name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Contract info must not exceed 500 characters")
    private String contractInfo;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;
}
