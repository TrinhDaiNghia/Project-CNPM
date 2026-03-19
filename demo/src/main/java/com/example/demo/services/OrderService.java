package com.example.demo.services;

import com.example.demo.dtos.request.OrderRequest;
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

    public Order createOrder(OrderRequest request) {
        accessControlService.requireCustomerAccess(request.getCustomerId());

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.getCustomerId()));

        Order order = Order.builder()
                .customer(customer)
                .note(request.getNote())
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






