package com.example.demo.dtos.response;

import com.example.demo.entities.enums.WarrantyStatus;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class WarrantyResponse {

    private String id;
    private String customerId;
    private String orderId;
    private String productId;
    private String productName;
    private Integer quantity;
    private String issueDescription;
    private WarrantyStatus status;
    private String resolutionNote;
    private Date requestedAt;
    private Date processedAt;
    private String processedByUserId;
    private String processedByUsername;
}
