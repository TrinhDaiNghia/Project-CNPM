package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardStatisticResponse {

    private List<RevenueByTimeResponse> revenueByTime;
    private List<OrdersByTimeResponse> ordersByDay;
    private List<OrdersByTimeResponse> ordersByMonth;
    private List<TopSellingProductResponse> topSellingProducts;
}

