package com.example.demo.dtos.request;

import com.example.demo.entities.enums.VoucherStatus;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

@Data
public class VoucherStatusUpdateRequest {

    private VoucherStatus status;

    private Boolean active;

    @AssertTrue(message = "Either status or active must be provided")
    public boolean isUpdatable() {
        return status != null || active != null;
    }
}

