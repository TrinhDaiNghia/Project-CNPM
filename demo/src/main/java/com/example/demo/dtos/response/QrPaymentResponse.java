package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QrPaymentResponse {
    private String orderId;
    private String accountNumber;
    private String bankCode;
    private Long amount;
    private String description;
    private String qrUrl;
}
