package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class ImportReceiptResponse {

    private String id;
    private Date importDate;
    private String note;
    private String supplierId;
    private String supplierName;
    private String ownerId;
    private List<ImportReceiptItemResponse> items;
    private Long totalAmount;
    private Long totalCost;
}
