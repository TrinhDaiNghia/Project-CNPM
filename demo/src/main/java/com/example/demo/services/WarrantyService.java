package com.example.demo.services;

import com.example.demo.dtos.DtoMapper;
import com.example.demo.dtos.request.WarrantyProcessRequest;
import com.example.demo.dtos.request.WarrantyRequest;
import com.example.demo.dtos.request.WarrantySearchRequest;
import com.example.demo.dtos.request.WarrantyStatusUpdateRequest;
import com.example.demo.dtos.response.WarrantyResponse;
import com.example.demo.entities.Customer;
import com.example.demo.entities.Notification;
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
import com.example.demo.repositories.NotificationRepository;
import com.example.demo.repositories.OrderRepository;
import com.example.demo.repositories.ProductRepository;
import com.example.demo.repositories.WarrantyRepository;
import lombok.RequiredArgsConstructor;

import org.hibernate.query.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class WarrantyService {

    private final WarrantyRepository warrantyRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final NotificationRepository notificationRepository;
    private final AccessControlService accessControlService;

    public WarrantyResponse createWarrantyRequest(WarrantyRequest request) {
        accessControlService.requirePrivilegedRole();

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));

        Warranty warranty = new Warranty();
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
}