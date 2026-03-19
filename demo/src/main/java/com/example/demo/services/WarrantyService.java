package com.example.demo.services;

import com.example.demo.dtos.request.WarrantyRequest;
import com.example.demo.dtos.request.WarrantyStatusUpdateRequest;
import com.example.demo.dtos.response.WarrantyResponse;
import com.example.demo.entities.Customer;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderItem;
import com.example.demo.entities.Product;
import com.example.demo.entities.User;
import com.example.demo.entities.Warranty;
import com.example.demo.entities.enums.OrderStatus;
import com.example.demo.entities.enums.UserRole;
import com.example.demo.entities.enums.WarrantyStatus;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.CustomerRepository;
import com.example.demo.repositories.OrderRepository;
import com.example.demo.repositories.ProductRepository;
import com.example.demo.repositories.WarrantyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WarrantyService {

    private final WarrantyRepository warrantyRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AccessControlService accessControlService;

    public WarrantyResponse createWarranty(WarrantyRequest request) {
        User currentUser = accessControlService.getCurrentUserOrThrow();
        if (currentUser.getRole() != UserRole.CUSTOMER) {
            throw new AccessDeniedException("Only CUSTOMER can create warranty requests");
        }

        accessControlService.requireCustomerAccess(request.getCustomerId());

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.getCustomerId()));
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("This order does not belong to the provided customer");
        }

        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException("Warranty is available only for completed orders");
        }

        boolean productInOrder = order.getOrderItems().stream()
                .map(OrderItem::getProduct)
                .anyMatch(itemProduct -> itemProduct.getId().equals(product.getId()));
        if (!productInOrder) {
            throw new IllegalArgumentException("This product is not found in the selected order");
        }

        boolean pendingExists = warrantyRepository.existsByCustomerIdAndOrderIdAndProductIdAndStatus(
                request.getCustomerId(),
                request.getOrderId(),
                request.getProductId(),
                WarrantyStatus.PENDING
        );
        if (pendingExists) {
            throw new IllegalStateException("A pending warranty request already exists for this product/order");
        }

        Warranty warranty = Warranty.builder()
                .customer(customer)
                .order(order)
                .product(product)
                .issueDescription(request.getIssueDescription().trim())
                .quantity(request.getQuantity())
                .status(WarrantyStatus.PENDING)
                .build();

        Warranty saved = warrantyRepository.save(warranty);
        return toResponse(saved);
    }

    public WarrantyResponse updateStatus(String id, WarrantyStatusUpdateRequest request) {
        accessControlService.requirePrivilegedRole();

        if (request.getStatus() == WarrantyStatus.PENDING) {
            throw new IllegalArgumentException("Warranty status can only be APPROVED or REJECTED");
        }
        if (request.getStatus() == WarrantyStatus.REJECTED && !StringUtils.hasText(request.getResolutionNote())) {
            throw new IllegalArgumentException("Resolution note is required when rejecting a warranty request");
        }

        Warranty warranty = warrantyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warranty not found: " + id));

        User handler = accessControlService.getCurrentUserOrThrow();
        warranty.setStatus(request.getStatus());
        warranty.setResolutionNote(trimToNull(request.getResolutionNote()));
        warranty.setProcessedAt(new Date());
        warranty.setProcessedBy(handler);

        Warranty saved = warrantyRepository.save(warranty);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public WarrantyResponse findById(String id) {
        Warranty warranty = warrantyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warranty not found: " + id));
        validateReadAccess(warranty);
        return toResponse(warranty);
    }

    @Transactional(readOnly = true)
    public List<WarrantyResponse> findAll(String customerId, WarrantyStatus status) {
        List<Warranty> warranties;
        if (StringUtils.hasText(customerId)) {
            accessControlService.requireCustomerAccess(customerId);
            warranties = status == null
                    ? warrantyRepository.findByCustomerId(customerId)
                    : warrantyRepository.findByCustomerIdAndStatus(customerId, status);
        } else {
            accessControlService.requirePrivilegedRole();
            warranties = status == null
                    ? warrantyRepository.findAll()
                    : warrantyRepository.findByStatus(status);
        }

        return warranties.stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateReadAccess(Warranty warranty) {
        User user = accessControlService.getCurrentUserOrThrow();
        if (user.getRole() == UserRole.CUSTOMER) {
            accessControlService.requireCustomerAccess(warranty.getCustomer().getId());
            return;
        }
        accessControlService.requirePrivilegedRole();
    }

    private WarrantyResponse toResponse(Warranty warranty) {
        return WarrantyResponse.builder()
                .id(warranty.getId())
                .customerId(warranty.getCustomer().getId())
                .orderId(warranty.getOrder().getId())
                .productId(warranty.getProduct().getId())
                .productName(warranty.getProduct().getName())
                .quantity(warranty.getQuantity())
                .issueDescription(warranty.getIssueDescription())
                .status(warranty.getStatus())
                .resolutionNote(warranty.getResolutionNote())
                .requestedAt(warranty.getRequestedAt())
                .processedAt(warranty.getProcessedAt())
                .processedByUserId(warranty.getProcessedBy() != null ? warranty.getProcessedBy().getId() : null)
                .processedByUsername(warranty.getProcessedBy() != null ? warranty.getProcessedBy().getUsername() : null)
                .build();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
