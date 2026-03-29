package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class ReportSummaryResponse {

    private Date fromDate;
    private Date toDate;
    private Long totalRevenue;
    private Long totalOrders;
    private Long totalProductsSold;
    private Long newCustomers;
    private List<DailyRevenueResponse> revenueByDay;
    private List<TopSellingProductResponse> topSellingProducts;
}
