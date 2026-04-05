package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DailyRevenueResponse {

    private String date;
    private Long revenue;
}
