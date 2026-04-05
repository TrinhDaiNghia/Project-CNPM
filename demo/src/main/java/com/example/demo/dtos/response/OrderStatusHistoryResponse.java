package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class OrderStatusHistoryResponse {

    private String status;
    private String note;
    private Date changedAt;
    private String changedBy;
}
