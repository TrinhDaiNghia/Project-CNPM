package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardSummaryResponse {

    private Long totalRevenue;
    private Long totalOrders;
    private Long newCustomers;
    private Long totalProductsSold;
}

