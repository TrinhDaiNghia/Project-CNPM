package com.example.demo.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class QrPaymentPrepareRequest {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;

    @Size(max = 255, message = "Shipping address must not exceed 255 characters")
    private String shippingAddress;

    private String voucherCode;

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<OrderRequest.OrderItemRequest> items;

    @Valid
    private OrderRequest.ShippingRequest shipping;
}
