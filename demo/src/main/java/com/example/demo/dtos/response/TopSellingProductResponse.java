package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopSellingProductResponse {

    private String productId;
    private String productName;
    private Long quantitySold;
    private Long revenue;
}
