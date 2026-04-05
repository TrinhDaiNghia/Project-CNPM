package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QrPaymentStatusResponse {
    private String orderId;
    private String status;
    private Long expectedAmount;
    private Long receivedAmount;
    private String message;
    private OrderResponse order;
}
