package com.example.demo.services;

import com.example.demo.entities.Order;
import com.example.demo.entities.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface OrderService {

    Order createOrder(Order order);

    Order updateOrderStatus(String id, OrderStatus status);

    Optional<Order> findById(String id);

    List<Order> findByCustomerId(String customerId);

    Page<Order> findByCustomerId(String customerId, Pageable pageable);

    List<Order> findByStatus(OrderStatus status);

    void cancelOrder(String id);
}
