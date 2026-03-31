package com.example.demo.dtos.response;

import com.example.demo.entities.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class OrderResponse {

    private String id;
    private Date orderDate;
    private Long totalAmount;
    private String note;
    private String shippingAddress;
    private OrderStatus status;
    private String customerId;
    private String customerUsername;
    private String voucherCode;
    private List<OrderItemResponse> orderItems;
}
