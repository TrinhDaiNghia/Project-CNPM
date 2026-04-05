package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CartResponse {

    private String id;
    private Long totalAmount;
    private String customerId;
    private List<CartItemResponse> items;
}
