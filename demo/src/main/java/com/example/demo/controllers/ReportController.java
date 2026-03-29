package com.example.demo.controllers;

import com.example.demo.dtos.response.DashboardReportResponse;
import com.example.demo.dtos.response.DashboardStatisticResponse;
import com.example.demo.dtos.response.DashboardSummaryResponse;
import com.example.demo.dtos.response.OrdersByTimeResponse;
import com.example.demo.dtos.response.RevenueByTimeResponse;
import com.example.demo.dtos.response.TopSellingProductResponse;
import com.example.demo.services.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardReportResponse> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date toDate,
            @RequestParam(required = false, defaultValue = "5") int topLimit) {
        DashboardReportResponse response = DashboardReportResponse.builder()
                .summary(reportService.getDashboardSummary(fromDate, toDate))
                .statistics(reportService.getDashboardStatistic(fromDate, toDate, topLimit))
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date toDate) {
        return ResponseEntity.ok(reportService.getDashboardSummary(fromDate, toDate));
    }

    @GetMapping("/statistics")
    public ResponseEntity<DashboardStatisticResponse> getStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date toDate,
            @RequestParam(required = false, defaultValue = "5") int topLimit) {
        return ResponseEntity.ok(reportService.getDashboardStatistic(fromDate, toDate, topLimit));
    }

    @GetMapping("/revenue")
    public ResponseEntity<List<RevenueByTimeResponse>> getRevenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date toDate) {
        return ResponseEntity.ok(reportService.getRevenueStatistics(fromDate, toDate));
    }

    @GetMapping("/orders/daily")
    public ResponseEntity<List<OrdersByTimeResponse>> getOrderStatsByDay(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date toDate) {
        return ResponseEntity.ok(reportService.getOrderStatisticsByDay(fromDate, toDate));
    }

    @GetMapping("/orders/monthly")
    public ResponseEntity<List<OrdersByTimeResponse>> getOrderStatsByMonth(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date toDate) {
        return ResponseEntity.ok(reportService.getOrderStatisticsByMonth(fromDate, toDate));
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<TopSellingProductResponse>> getTopProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date toDate,
            @RequestParam(required = false, defaultValue = "5") int topLimit) {
        return ResponseEntity.ok(reportService.getTopSellingProducts(fromDate, toDate, topLimit));
    }
}

