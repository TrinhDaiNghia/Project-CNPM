package com.example.demo.dtos.request;

import com.example.demo.entities.enums.WarrantyStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;

@Data
public class WarrantyRequest {

    @NotBlank(message = "Customer phone is required")
    @Size(max = 20)
    private String customerPhone;

    @NotBlank(message = "Customer name is required")
    @Size(max = 100)
    private String customerName;

    private String customerId;

    @NotBlank(message = "Issue description is required")
    @Size(max = 1000)
    private String issueDescription;

    @NotNull(message = "Received date is required")
    private Date receivedDate;

    @NotNull(message = "Expected return date is required")
    private Date expectedReturnDate;

    private WarrantyStatus status;

    @Size(max = 1000)
    private String technicianNote;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotBlank(message = "Product ID is required")
    private String productId;
}

