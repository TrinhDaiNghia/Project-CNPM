package com.example.demo.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;

    @Size(max = 255, message = "Shipping address must not exceed 255 characters")
    private String shippingAddress;

    private String voucherCode;

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {

        @NotBlank(message = "Product ID is required")
        private String productId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }
}
