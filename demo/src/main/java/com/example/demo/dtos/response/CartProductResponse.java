package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartProductResponse {

    private String id;
    private String name;
    private Long price;
}
