package com.example.demo.services.impl;

import com.example.demo.entities.Order;
import com.example.demo.entities.enums.OrderStatus;
import com.example.demo.repositories.OrderRepository;
import com.example.demo.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Override
    public Order createOrder(Order order) {
        return orderRepository.save(order);
    }

    @Override
    public Order updateOrderStatus(String id, OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));

        OrderStatus currentStatus = order.getStatus();
        if (currentStatus == status){
            return order;
        }

        if (!isValidTransition(currentStatus, status)) {
            throw new IllegalStateException("Invalid status transition from " + currentStatus + " to " + status);
        }


        order.setStatus(status);
        return orderRepository.save(order);
    }

    private boolean isValidTransition(OrderStatus from, OrderStatus to){
        return switch (from){
            case PENDING -> EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED).contains(to);
            case CONFIRMED -> EnumSet.of(OrderStatus.SHIPPING, OrderStatus.CANCELLED).contains(to);
            case SHIPPING -> EnumSet.of(OrderStatus.DELIVERED, OrderStatus.RETURNED).contains(to);
            case DELIVERED -> EnumSet.of(OrderStatus.COMPLETED, OrderStatus.RETURNED).contains(to);
            case COMPLETED, CANCELLED, RETURNED -> false;
        };
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(String id) {
        return orderRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findByCustomerId(String customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> findByCustomerId(String customerId, Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    @Override
    public void cancelOrder(String id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel an order with status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }
}
