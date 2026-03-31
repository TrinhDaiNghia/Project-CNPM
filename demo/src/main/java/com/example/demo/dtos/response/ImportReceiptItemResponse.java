package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImportReceiptItemResponse {

    private String id;
    private String productId;
    private String productName;
    private Integer quantity;
    private Long importPrice;
    private Long lineTotal;
}
