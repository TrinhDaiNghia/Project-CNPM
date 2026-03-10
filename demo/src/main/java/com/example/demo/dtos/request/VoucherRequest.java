package com.example.demo.dtos.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Date;

@Data
public class VoucherRequest {

    @NotBlank(message = "Voucher code is required")
    @Size(min = 4, max = 50)
    private String voucherCode;

    @Min(0) @Max(100)
    private Integer discountPercent = 0;

    @Min(0)
    private Long minOrderAmount = 0L;

    @NotNull(message = "Valid from date is required")
    private Date validFrom;

    @NotNull(message = "Valid to date is required")
    private Date validTo;

    @Min(1)
    private Integer maxUsage = 1;
}
