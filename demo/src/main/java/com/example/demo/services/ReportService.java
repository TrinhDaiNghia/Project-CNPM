package com.example.demo.services;

import com.example.demo.dtos.response.DailyRevenueResponse;
import com.example.demo.dtos.response.ReportSummaryResponse;
import com.example.demo.dtos.response.TopSellingProductResponse;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderItem;
import com.example.demo.entities.enums.OrderStatus;
import com.example.demo.repositories.CustomerRepository;
import com.example.demo.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final AccessControlService accessControlService;

    public ReportSummaryResponse getSummary(Date fromDate, Date toDate) {
        accessControlService.requireOwnerRole();
        validateDateRange(fromDate, toDate);

        Date start = normalizeStart(fromDate);
        Date end = normalizeEnd(toDate);

        List<Order> filteredOrders = orderRepository.findAll().stream()
                .filter(order -> isRevenueStatus(order.getStatus()))
                .filter(order -> isInDateRange(order.getOrderDate(), start, end))
                .toList();

        long totalRevenue = filteredOrders.stream()
                .mapToLong(order -> order.getTotalAmount() == null ? 0L : order.getTotalAmount())
                .sum();

        long totalProductsSold = filteredOrders.stream()
                .flatMap(order -> order.getOrderItems().stream())
                .mapToLong(item -> item.getQuantity() == null ? 0L : item.getQuantity())
                .sum();

        long newCustomers = (start == null && end == null)
                ? customerRepository.count()
                : customerRepository.countByUserCreatedAtBetween(
                        start == null ? new Date(0) : start,
                        end == null ? new Date() : end
                );

        List<DailyRevenueResponse> dailyRevenue = buildDailyRevenue(filteredOrders);
        List<TopSellingProductResponse> topProducts = buildTopProducts(filteredOrders, 10);

        return ReportSummaryResponse.builder()
                .fromDate(start)
                .toDate(end)
                .totalRevenue(totalRevenue)
                .totalOrders((long) filteredOrders.size())
                .totalProductsSold(totalProductsSold)
                .newCustomers(newCustomers)
                .revenueByDay(dailyRevenue)
                .topSellingProducts(topProducts)
                .build();
    }

    private List<DailyRevenueResponse> buildDailyRevenue(List<Order> orders) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, Long> revenueByDay = new HashMap<>();

        for (Order order : orders) {
            if (order.getOrderDate() == null) {
                continue;
            }
            String dayKey = formatter.format(order.getOrderDate());
            long amount = order.getTotalAmount() == null ? 0L : order.getTotalAmount();
            revenueByDay.put(dayKey, revenueByDay.getOrDefault(dayKey, 0L) + amount);
        }

        return revenueByDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> DailyRevenueResponse.builder()
                        .date(entry.getKey())
                        .revenue(entry.getValue())
                        .build())
                .toList();
    }

    private List<TopSellingProductResponse> buildTopProducts(List<Order> orders, int limit) {
        Map<String, ProductAggregate> aggregateMap = new HashMap<>();

        for (Order order : orders) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getProduct() == null) {
                    continue;
                }

                String productId = item.getProduct().getId();
                ProductAggregate aggregate = aggregateMap.computeIfAbsent(
                        productId,
                        ignored -> new ProductAggregate(productId, item.getProduct().getName(), 0L, 0L)
                );

                long quantity = item.getQuantity() == null ? 0L : item.getQuantity();
                long subTotal = item.getSubTotal() == null
                        ? (item.getProduct().getPrice() == null ? 0L : item.getProduct().getPrice() * quantity)
                        : item.getSubTotal();

                aggregate.quantitySold += quantity;
                aggregate.revenue += subTotal;
            }
        }

        return aggregateMap.values().stream()
                .sorted(
                        Comparator.comparingLong((ProductAggregate value) -> value.quantitySold).reversed()
                                .thenComparing(Comparator.comparingLong((ProductAggregate value) -> value.revenue)
                                        .reversed())
                )
                .limit(limit)
                .map(aggregate -> TopSellingProductResponse.builder()
                        .productId(aggregate.productId)
                        .productName(aggregate.productName)
                        .quantitySold(aggregate.quantitySold)
                        .revenue(aggregate.revenue)
                        .build())
                .toList();
    }

    private boolean isRevenueStatus(OrderStatus status) {
        return status == OrderStatus.DELIVERED || status == OrderStatus.COMPLETED;
    }

    private boolean isInDateRange(Date date, Date start, Date end) {
        if (date == null) {
            return false;
        }
        if (start != null && date.before(start)) {
            return false;
        }
        return end == null || !date.after(end);
    }

    private Date normalizeStart(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date normalizeEnd(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    private void validateDateRange(Date fromDate, Date toDate) {
        if (fromDate != null && toDate != null && fromDate.after(toDate)) {
            throw new IllegalArgumentException("fromDate must be before or equal to toDate");
        }
    }

    private static class ProductAggregate {
        private final String productId;
        private final String productName;
        private long quantitySold;
        private long revenue;

        private ProductAggregate(String productId, String productName, long quantitySold, long revenue) {
            this.productId = productId;
            this.productName = productName;
            this.quantitySold = quantitySold;
            this.revenue = revenue;
        }
    }
}
