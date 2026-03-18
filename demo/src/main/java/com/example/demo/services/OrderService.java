package com.example.demo.services;

import com.example.demo.entities.Order;
import com.example.demo.entities.enums.OrderStatus;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final AccessControlService accessControlService;

    public Order createOrder(Order order) {
        if (order.getCustomer() == null || order.getCustomer().getId() == null) {
            throw new IllegalArgumentException("Customer is required to create an order");
        }
        accessControlService.requireCustomerAccess(order.getCustomer().getId());
        return orderRepository.save(order);
    }

    public Order updateOrderStatus(String id, OrderStatus status) {
        accessControlService.requirePrivilegedRole();
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Use /api/orders/{id}/cancel to cancel an order");
        }

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        order.setStatus(status);
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public Optional<Order> findById(String id) {
        return orderRepository.findById(id)
                .map(order -> {
                    accessControlService.requireCustomerAccess(order.getCustomer().getId());
                    return order;
                });
    }

    @Transactional(readOnly = true)
    public List<Order> findByCustomerId(String customerId) {
        accessControlService.requireCustomerAccess(customerId);
        return orderRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public Page<Order> findByCustomerId(String customerId, Pageable pageable) {
        accessControlService.requireCustomerAccess(customerId);
        return orderRepository.findByCustomerId(customerId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Order> findByStatus(OrderStatus status) {
        accessControlService.requirePrivilegedRole();
        return orderRepository.findByStatus(status);
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





