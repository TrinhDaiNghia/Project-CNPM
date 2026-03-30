package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardReportResponse {

    private DashboardSummaryResponse summary;
    private DashboardStatisticResponse statistics;
}

