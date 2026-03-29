package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrdersByTimeResponse {

    private String time;
    private Long totalOrders;
}

