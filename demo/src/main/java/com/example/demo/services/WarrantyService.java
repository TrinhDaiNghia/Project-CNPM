package com.example.demo.services;

import com.example.demo.dtos.DtoMapper;
import com.example.demo.dtos.request.CustomerWarrantyRequest;
import com.example.demo.dtos.request.WarrantyProcessRequest;
import com.example.demo.dtos.request.WarrantyRequest;
import com.example.demo.dtos.request.WarrantySearchRequest;
import com.example.demo.dtos.response.WarrantyResponse;
import com.example.demo.entities.Customer;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderItem;
import com.example.demo.entities.Notification;
import com.example.demo.entities.Product;
import com.example.demo.entities.User;
import com.example.demo.entities.Warranty;
import com.example.demo.entities.enums.OrderStatus;
import com.example.demo.entities.enums.UserRole;
import com.example.demo.entities.enums.NotificationType;
import com.example.demo.entities.enums.WarrantyStatus;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.CustomerRepository;
import com.example.demo.repositories.OrderRepository;
import com.example.demo.repositories.NotificationRepository;
import com.example.demo.repositories.ProductRepository;
import com.example.demo.repositories.WarrantyRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Transactional
public class WarrantyService {

    private final WarrantyRepository warrantyRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final com.example.demo.repositories.UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final AccessControlService accessControlService;

    public WarrantyResponse createWarrantyRequest(WarrantyRequest request) {
        accessControlService.requirePrivilegedRole();

        String customerId = resolveCustomerId(request);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));

        Warranty warranty = new Warranty();
        warranty.setUserId(null);
        warranty.setCustomerId(customerId);
        warranty.setOrderId(null);
        warranty.setOrderItemId(null);
        warranty.setCustomerPhone(request.getCustomerPhone().trim());
        warranty.setCustomerName(request.getCustomerName().trim());
        warranty.setIssueDescription(request.getIssueDescription().trim());
        warranty.setReceivedDate(request.getReceivedDate());
        warranty.setExpectedReturnDate(request.getExpectedReturnDate());
        warranty.setQuantity(request.getQuantity());
        warranty.setTechnicianNote(normalizeBlankToNull(request.getTechnicianNote()));
        warranty.setStatus(request.getStatus() == null ? WarrantyStatus.RECEIVED : request.getStatus());
        warranty.setProduct(product);

        return DtoMapper.toWarrantyResponse(warrantyRepository.save(warranty));
    }

    public WarrantyResponse createCustomerWarrantyRequest(CustomerWarrantyRequest request) {
        User currentUser = accessControlService.getCurrentUserOrThrow();
        if (currentUser.getRole() != com.example.demo.entities.enums.UserRole.CUSTOMER) {
            throw new org.springframework.security.access.AccessDeniedException("Only CUSTOMER is allowed for this action");
        }

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));

        if (!order.getCustomer().getId().equals(currentUser.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("You are not allowed to create warranty for this order");
        }

        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException("Chỉ tạo bảo hành cho đơn đã giao hoặc hoàn tất.");
        }

        OrderItem selectedItem = order.getOrderItems().stream()
                .filter(item -> item.getId().equals(request.getOrderItemId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found: " + request.getOrderItemId()));

        Warranty warranty = new Warranty();
        warranty.setUserId(currentUser.getId());
        warranty.setCustomerId(currentUser.getId());
        warranty.setOrderId(order.getId());
        warranty.setOrderItemId(selectedItem.getId());
        warranty.setCustomerPhone(order.getCustomer().getPhone().trim());
        warranty.setCustomerName(order.getCustomer().getFullName().trim());
        warranty.setIssueDescription(request.getDescription().trim());
        warranty.setReceivedDate(new Date());
        warranty.setExpectedReturnDate(new Date(System.currentTimeMillis() + 2L * 24 * 60 * 60 * 1000));
        warranty.setQuantity(selectedItem.getQuantity());
        warranty.setTechnicianNote(null);
        warranty.setStatus(WarrantyStatus.RECEIVED);
        warranty.setProduct(selectedItem.getProduct());

        Warranty saved = warrantyRepository.save(warranty);
        notifyCustomerIfPossible(saved, currentUser, saved.getStatus());
        return DtoMapper.toWarrantyResponse(saved);
    }

    @Transactional(readOnly = true)
    public Optional<WarrantyResponse> getWarrantyRequestDetail(String id) {
        accessControlService.requirePrivilegedRole();
        return warrantyRepository.findById(id).map(DtoMapper::toWarrantyResponse);
    }

    @Transactional(readOnly = true)
    public Page<WarrantyResponse> searchWarrantyRequests(WarrantySearchRequest request, Pageable pageable) {
        accessControlService.requirePrivilegedRole();
        return warrantyRepository.searchWarrantyRequests(normalizeSearchText(request.getKeyword()), request.getStatus(), pageable)
                .map(DtoMapper::toWarrantyResponse);
    }

    @Transactional(readOnly = true)
    public Page<WarrantyResponse> getMyWarrantyRequests(Pageable pageable) {
        User currentUser = accessControlService.getCurrentUserOrThrow();
        if (currentUser.getRole() != com.example.demo.entities.enums.UserRole.CUSTOMER) {
            throw new org.springframework.security.access.AccessDeniedException("Only CUSTOMER is allowed for this action");
        }

        return warrantyRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId(), pageable)
                .map(DtoMapper::toWarrantyResponse);
    }

    public WarrantyResponse approveWarrantyRequest(String id, String technicianNote) {
        WarrantyProcessRequest processRequest = new WarrantyProcessRequest();
        processRequest.setStatus(WarrantyStatus.COMPLETED);
        processRequest.setTechnicianNote(technicianNote);
        return processWarrantyRequest(id, processRequest);
    }

    public WarrantyResponse rejectWarrantyRequest(String id, String rejectReason) {
        WarrantyProcessRequest processRequest = new WarrantyProcessRequest();
        processRequest.setStatus(WarrantyStatus.REJECTED);
        processRequest.setRejectReason(rejectReason);
        return processWarrantyRequest(id, processRequest);
    }

    public WarrantyResponse processWarrantyRequest(String id, WarrantyProcessRequest request) {
        accessControlService.requirePrivilegedRole();

        Warranty warranty = warrantyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warranty request not found: " + id));

        validateTransition(warranty.getStatus(), request.getStatus());
        if (request.getStatus() == WarrantyStatus.REJECTED
                && (request.getRejectReason() == null || request.getRejectReason().isBlank())) {
            throw new IllegalArgumentException("Reject reason is required when rejecting warranty request");
        }

        User processor = accessControlService.getCurrentUserOrThrow();
        String processedNote = buildProcessedNote(request, processor);
        String processedRejectReason = buildProcessedRejectReason(request, processor);

        warranty.setStatus(request.getStatus());
        warranty.setTechnicianNote(processedNote);
        warranty.setRejectReason(processedRejectReason);

        Warranty saved = warrantyRepository.save(warranty);
        notifyCustomerIfPossible(saved, processor, request.getStatus());
        return DtoMapper.toWarrantyResponse(saved);
    }

    private void validateTransition(WarrantyStatus currentStatus, WarrantyStatus nextStatus) {
        if (nextStatus == null) {
            throw new IllegalArgumentException("Warranty status is required");
        }
        if (currentStatus == WarrantyStatus.COMPLETED || currentStatus == WarrantyStatus.REJECTED) {
            throw new IllegalStateException("Cannot process warranty request with final status: " + currentStatus);
        }
    }

    private String buildProcessedNote(WarrantyProcessRequest request, User processor) {
        String actor = processor.getFullName() == null || processor.getFullName().isBlank()
                ? processor.getUsername()
                : processor.getFullName();

        if (request.getTechnicianNote() != null && !request.getTechnicianNote().isBlank()) {
            return "Processed by " + actor + ": " + request.getTechnicianNote().trim();
        }

        return "Processed by " + actor;
    }

    private String buildProcessedRejectReason(WarrantyProcessRequest request, User processor) {
        if (request.getStatus() != WarrantyStatus.REJECTED) {
            return null;
        }

        String actor = processor.getFullName() == null || processor.getFullName().isBlank()
                ? processor.getUsername()
                : processor.getFullName();

        return "Rejected by " + actor + ": " + request.getRejectReason().trim();
    }

    private void notifyCustomerIfPossible(Warranty warranty, User sender, WarrantyStatus status) {
        Optional<Customer> customerOpt = customerRepository.findByPhone(warranty.getCustomerPhone());
        if (customerOpt.isEmpty()) {
            return;
        }

        String content = status == WarrantyStatus.REJECTED
                ? "Your warranty request has been rejected. Please check details from support."
                : "Your warranty request has been updated to status: " + status;

        Notification notification = Notification.builder()
                .title("Warranty request update")
                .content(content)
                .type(NotificationType.WARRANTY)
                .directUrl("/warranty")
                .sender(sender)
                .receiver(customerOpt.get())
                .build();

        notificationRepository.save(notification);
    }

    private String normalizeSearchText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeBlankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String resolveCustomerId(WarrantyRequest request) {
        if (request.getCustomerId() != null && !request.getCustomerId().isBlank()) {
            return request.getCustomerId().trim();
        }

        if (request.getCustomerPhone() == null || request.getCustomerPhone().isBlank()) {
            throw new IllegalArgumentException("Customer ID is required");
        }

        User customerUser = userRepository.findByPhone(request.getCustomerPhone().trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found by phone: " + request.getCustomerPhone()));

        if (customerUser.getRole() != UserRole.CUSTOMER) {
            throw new ResourceNotFoundException("Customer not found by phone: " + request.getCustomerPhone());
        }

        return customerUser.getId();
    }
}
