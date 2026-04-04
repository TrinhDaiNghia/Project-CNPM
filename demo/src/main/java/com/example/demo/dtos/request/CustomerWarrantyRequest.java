package com.example.demo.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CustomerWarrantyRequest {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotBlank(message = "Order item ID is required")
    private String orderItemId;

    @NotBlank(message = "Issue description is required")
    @Size(max = 1000)
    private String description;

    @NotNull
    private List<String> images;
}