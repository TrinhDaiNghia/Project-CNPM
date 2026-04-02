package com.example.demo.services;

import com.example.demo.dtos.request.OrderRequest;
import com.example.demo.dtos.response.OrderItemResponse;
import com.example.demo.dtos.response.OrderResponse;
import com.example.demo.entities.Customer;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderItem;
import com.example.demo.entities.Product;
import com.example.demo.entities.Voucher;
import com.example.demo.entities.enums.OrderStatus;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.CustomerRepository;
import com.example.demo.repositories.OrderRepository;
import com.example.demo.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final VoucherService voucherService;
    private final AccessControlService accessControlService;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        accessControlService.requireCustomerAccess(request.getCustomerId());

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.getCustomerId()));

        Order order = Order.builder()
                .customer(customer)
                .note(request.getNote())
                .shippingAddress(request.getShippingAddress())
                .build();

        // Build order items
        List<OrderItem> orderItems = new ArrayList<>();
        long totalAmount = 0;
        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemReq.getProductId()));
            long subTotal = product.getPrice() * itemReq.getQuantity();
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .subTotal(subTotal)
                    .build();
            orderItems.add(orderItem);
            totalAmount += subTotal;
        }
        order.setOrderItems(orderItems);

        // Apply voucher if provided
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            Voucher voucher = voucherService.consumeVoucher(request.getVoucherCode());
            order.setVoucher(voucher);
            long discount = totalAmount * voucher.getDiscountPercent() / 100;
            totalAmount -= discount;
        }

        order.setTotalAmount(Math.max(totalAmount, 0L));
        return toOrderResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateOrderStatus(String id, OrderStatus status) {
        accessControlService.requirePrivilegedRole();
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Use /api/orders/{id}/cancel to cancel an order");
        }

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        order.setStatus(status);
        
        return toOrderResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public Optional<OrderResponse> findById(String id) {
        return orderRepository.findById(id)
                .map(order -> {
                    accessControlService.requireCustomerAccess(order.getCustomer().getId());
                    return toOrderResponse(order);
                });
    }

    @Transactional(readOnly = true)
    public List<Order> findByCustomerId(String customerId) {
        accessControlService.requireCustomerAccess(customerId);
        return orderRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findByCustomerId(String customerId, Pageable pageable) {
        accessControlService.requireCustomerAccess(customerId);
        Page<Order> orderPage = orderRepository.findByCustomerId(customerId, pageable);
        List<OrderResponse> responses = orderPage.getContent().stream()
                .map(this::toOrderResponse)
                .toList();
        return new PageImpl<>(responses, pageable, orderPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<Order> findByStatus(OrderStatus status) {
        accessControlService.requirePrivilegedRole();
        return orderRepository.findByStatus(status);
    }

    public OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .subTotal(item.getSubTotal())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .orderDate(order.getOrderDate())
                .totalAmount(order.getTotalAmount())
                .note(order.getNote())
                .shippingAddress(order.getShippingAddress())
                .status(order.getStatus())
                .customerId(order.getCustomer().getId())
                .customerUsername(order.getCustomer().getUsername())
                .customerFullName(order.getCustomer().getFullName())
                .customerPhone(order.getCustomer().getPhone())
                .customerAddress(order.getCustomer().getAddress())
                .voucherCode(order.getVoucher() != null ? order.getVoucher().getCode() : null)
                .orderItems(itemResponses)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findAllOrders(Pageable pageable) {
        accessControlService.requirePrivilegedRole();
        Page<Order> orderPage = orderRepository.findAll(pageable);
        List<OrderResponse> responses = orderPage.getContent().stream()
                .map(this::toOrderResponse)
                .toList();
        return new PageImpl<>(responses, pageable, orderPage.getTotalElements());
    }

    public void cancelOrder(String id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

        accessControlService.requireCustomerAccess(order.getCustomer().getId());
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel an order with status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }
}