package com.example.demo.dtos.response;

import com.example.demo.entities.enums.WarrantyStatus;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class WarrantyResponse {

    private String id;
    private String customerPhone;
    private String customerName;
    private String issueDescription;
    private Date receivedDate;
    private Date expectedReturnDate;
    private WarrantyStatus status;
    private String technicianNote;
    private String rejectReason;
    private Integer quantity;
    private String productId;
    private String productName;
}

