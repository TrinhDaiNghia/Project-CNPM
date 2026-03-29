package com.example.demo.dtos.request;

import com.example.demo.entities.enums.VoucherStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VoucherSearchRequest {

    @Size(max = 100)
    private String keyword;

    private VoucherStatus status;

    private Boolean active;
}

