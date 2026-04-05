package com.example.demo.dtos.request;

import com.example.demo.entities.enums.WarrantyStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WarrantyProcessRequest {

    @NotNull(message = "Warranty status is required")
    private WarrantyStatus status;

    @Size(max = 1000)
    private String rejectReason;

    @Size(max = 1000)
    private String technicianNote;

    @AssertTrue(message = "Reject reason is required when status is REJECTED")
    public boolean isRejectReasonValid() {
        if (status != WarrantyStatus.REJECTED) {
            return true;
        }
        return rejectReason != null && !rejectReason.isBlank();
    }
}

