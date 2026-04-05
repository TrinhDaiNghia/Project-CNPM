package com.example.demo.dtos.request;

import com.example.demo.entities.enums.WarrantyStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WarrantyStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private WarrantyStatus status;

    @Size(max = 1000, message = "Resolution note must not exceed 1000 characters")
    private String resolutionNote;
}
