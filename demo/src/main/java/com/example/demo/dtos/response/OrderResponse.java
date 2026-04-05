package com.example.demo.dtos.response;

import com.example.demo.entities.enums.OrderStatus;
import com.example.demo.entities.enums.PaymentMethod;
import com.example.demo.entities.enums.PaymentStatus;
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
    private String customerFullName;
    private String customerPhone;
    private String customerAddress;
    private String voucherCode;
    private List<OrderItemResponse> orderItems;
    private List<OrderStatusHistoryResponse> timeline;

    private PaymentResponse payment;

    private boolean canCancel;
    private boolean canRequestCancel;
    private boolean refundRequired;
    private String refundMessage;
    private String cancellationReason;
    private String cancellationNote;

    @Data
    @Builder
    public static class PaymentResponse {
        private PaymentMethod method;
        private PaymentStatus status;
        private Boolean isPaid;
        private Date paymentDate;
        private Long amount;
    }

}
