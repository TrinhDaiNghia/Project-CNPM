package com.example.demo.dtos.request;

import com.example.demo.entities.enums.VoucherStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Date;

@Data
public class VoucherRequest {

    @NotBlank(message = "Voucher code is required")
    @Size(min = 4, max = 50)
    private String code;

    @Min(0) @Max(100)
    private Integer discountPercent = 0;

    private Boolean isUsed = false;

    @NotNull(message = "Valid from date is required")
    private Date validFrom;

    @NotNull(message = "Valid to date is required")
    private Date validTo;

    @Min(1)
    private Integer quantity = 1;

    private Date usedAt;

    private VoucherStatus status;

    @AssertTrue(message = "Valid to date must be after valid from date")
    public boolean isValidDateRange() {
        if (validFrom == null || validTo == null) {
            return true;
        }
        return validTo.after(validFrom);
    }
}
