package com.example.demo.services;

import com.example.demo.dtos.request.ReportFilterRequest;
import com.example.demo.dtos.response.DashboardReportResponse;
import com.example.demo.dtos.response.DashboardStatisticResponse;
import com.example.demo.dtos.response.DashboardSummaryResponse;
import com.example.demo.dtos.response.OrdersByTimeResponse;
import com.example.demo.dtos.response.RevenueByTimeResponse;
import com.example.demo.dtos.response.TopSellingProductResponse;
import com.example.demo.repositories.CustomerRepository;
import com.example.demo.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final AccessControlService accessControlService;

    public DashboardReportResponse getDashboardReport(ReportFilterRequest filterRequest) {
        accessControlService.requirePrivilegedRole();
        Date fromDate = filterRequest != null ? filterRequest.getFromDate() : null;
        Date toDate = filterRequest != null ? filterRequest.getToDate() : null;

        return DashboardReportResponse.builder()
                .summary(getDashboardSummary(fromDate, toDate))
                .statistics(getDashboardStatistic(fromDate, toDate, 5))
                .build();
    }

    public DashboardSummaryResponse getDashboardSummary(Date fromDate, Date toDate) {
        accessControlService.requirePrivilegedRole();
        try {
            Long totalRevenue = nonNullLong(orderRepository.sumRevenueBetween(fromDate, toDate));
            long totalOrders = orderRepository.countOrdersBetween(fromDate, toDate);
            long newCustomers = customerRepository.countNewCustomersBetween(fromDate, toDate);
            Long totalProductsSold = nonNullLong(orderRepository.sumSoldQuantityBetween(fromDate, toDate));

            return DashboardSummaryResponse.builder()
                    .totalRevenue(totalRevenue)
                    .totalOrders(totalOrders)
                    .newCustomers(newCustomers)
                    .totalProductsSold(totalProductsSold)
                    .build();
        } catch (RuntimeException ex) {
            log.error("Failed to query dashboard summary from {} to {}", fromDate, toDate, ex);
            throw new IllegalStateException("Cannot generate dashboard summary at the moment");
        }
    }

    public DashboardStatisticResponse getDashboardStatistic(Date fromDate, Date toDate, int topLimit) {
        accessControlService.requirePrivilegedRole();
        try {
            return DashboardStatisticResponse.builder()
                    .revenueByTime(getRevenueStatistics(fromDate, toDate))
                    .ordersByDay(getOrderStatisticsByDay(fromDate, toDate))
                    .ordersByMonth(getOrderStatisticsByMonth(fromDate, toDate))
                    .topSellingProducts(getTopSellingProducts(fromDate, toDate, topLimit))
                    .build();
        } catch (RuntimeException ex) {
            log.error("Failed to query dashboard statistics from {} to {}", fromDate, toDate, ex);
            throw new IllegalStateException("Cannot generate dashboard statistics at the moment");
        }
    }

    public List<RevenueByTimeResponse> getRevenueStatistics(Date fromDate, Date toDate) {
        accessControlService.requirePrivilegedRole();
        try {
            List<Object[]> rows = orderRepository.sumRevenueByDay(fromDate, toDate);
            if (rows == null || rows.isEmpty()) {
                return Collections.emptyList();
            }
            return rows.stream()
                    .map(row -> RevenueByTimeResponse.builder()
                            .time(valueAsString(row[0]))
                            .revenue(valueAsLong(row[1]))
                            .build())
                    .toList();
        } catch (RuntimeException ex) {
            log.error("Failed to query revenue statistics from {} to {}", fromDate, toDate, ex);
            throw new IllegalStateException("Cannot query revenue statistics");
        }
    }

    public List<OrdersByTimeResponse> getOrderStatisticsByDay(Date fromDate, Date toDate) {
        accessControlService.requirePrivilegedRole();
        try {
            List<Object[]> rows = orderRepository.countOrdersByDay(fromDate, toDate);
            if (rows == null || rows.isEmpty()) {
                return Collections.emptyList();
            }
            return rows.stream()
                    .map(row -> OrdersByTimeResponse.builder()
                            .time(valueAsString(row[0]))
                            .totalOrders(valueAsLong(row[1]))
                            .build())
                    .toList();
        } catch (RuntimeException ex) {
            log.error("Failed to query order statistics by day from {} to {}", fromDate, toDate, ex);
            throw new IllegalStateException("Cannot query daily order statistics");
        }
    }

    public List<OrdersByTimeResponse> getOrderStatisticsByMonth(Date fromDate, Date toDate) {
        accessControlService.requirePrivilegedRole();
        try {
            List<Object[]> rows = orderRepository.countOrdersByMonth(fromDate, toDate);
            if (rows == null || rows.isEmpty()) {
                return Collections.emptyList();
            }
            return rows.stream()
                    .map(row -> OrdersByTimeResponse.builder()
                            .time(valueAsString(row[0]))
                            .totalOrders(valueAsLong(row[1]))
                            .build())
                    .toList();
        } catch (RuntimeException ex) {
            log.error("Failed to query order statistics by month from {} to {}", fromDate, toDate, ex);
            throw new IllegalStateException("Cannot query monthly order statistics");
        }
    }

    public List<TopSellingProductResponse> getTopSellingProducts(Date fromDate, Date toDate, int topLimit) {
        accessControlService.requirePrivilegedRole();
        try {
            int safeLimit = topLimit <= 0 ? 5 : topLimit;
            Pageable pageable = PageRequest.of(0, safeLimit);
            return orderRepository.findTopSellingProducts(fromDate, toDate, pageable)
                    .stream()
                    .map(row -> TopSellingProductResponse.builder()
                            .productId(valueAsString(row[0]))
                            .productName(valueAsString(row[1]))
                            .soldQuantity(valueAsLong(row[2]))
                            .build())
                    .toList();
        } catch (RuntimeException ex) {
            log.error("Failed to query top-selling products from {} to {}", fromDate, toDate, ex);
            throw new IllegalStateException("Cannot query top-selling products");
        }
    }

    private String valueAsString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long valueAsLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Long nonNullLong(Long value) {
        return value == null ? 0L : value;
    }
}

