package com.example.demo.dtos.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupplierSearchRequest {

    @Size(max = 100)
    private String keyword;

    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String contractInfo;

    @Size(max = 255)
    private String address;
}

