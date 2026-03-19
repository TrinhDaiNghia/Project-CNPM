package com.example.demo.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WarrantyRequest {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotBlank(message = "Issue description is required")
    @Size(max = 1000, message = "Issue description must not exceed 1000 characters")
    private String issueDescription;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
