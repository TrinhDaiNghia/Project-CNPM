package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartItemResponse {
    private String id;
    private Integer quantity;
    private Long subTotal;
    private CartProductResponse product;
}

