package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RevenueByTimeResponse {

    private String time;
    private Long revenue;
}

