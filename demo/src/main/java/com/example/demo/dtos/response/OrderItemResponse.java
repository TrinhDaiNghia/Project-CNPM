package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderItemResponse {

    private String id;
    private String productId;
    private String productName;
    private Integer quantity;
    private Long subTotal;
}
