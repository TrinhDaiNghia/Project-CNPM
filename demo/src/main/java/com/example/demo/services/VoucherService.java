package com.example.demo.services;

import com.example.demo.dtos.request.VoucherCreateRequest;
import com.example.demo.dtos.request.VoucherSearchRequest;
import com.example.demo.dtos.request.VoucherStatusUpdateRequest;
import com.example.demo.dtos.request.VoucherUpdateRequest;
import com.example.demo.entities.Voucher;
import com.example.demo.entities.enums.VoucherStatus;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final AccessControlService accessControlService;

    public Voucher createVoucher(VoucherCreateRequest request) {
        accessControlService.requireOwnerRole();
        validateVoucherCreate(request);
        if (voucherRepository.existsByCode(request.getCode().trim())) {
            throw new IllegalStateException("Voucher code already exists");
        }

        Voucher voucher = new Voucher();
        applyVoucherRequest(voucher, request);
        return voucherRepository.save(voucher);
    }

    public Voucher updateVoucher(String id, VoucherUpdateRequest request) {
        accessControlService.requireOwnerRole();
        validateVoucherUpdate(request);

        Voucher existing = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found: " + id));

        if (voucherRepository.existsByCodeAndIdNot(request.getCode().trim(), id)) {
            throw new IllegalStateException("Voucher code already exists");
        }

        applyVoucherRequest(existing, request);
        return voucherRepository.save(existing);
    }

    public void deleteVoucher(String id) {
        accessControlService.requireOwnerRole();
        Voucher existing = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found: " + id));

        boolean active = existing.getStatus() == VoucherStatus.ACTIVE;
        boolean usedInOrder = voucherRepository.existsUsedInOrders(id);

        if (active || usedInOrder) {
            disableVoucher(id);
            return;
        }

        voucherRepository.delete(existing);
    }

    @Transactional(readOnly = true)
    public Optional<Voucher> findById(String id) {
        return voucherRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Voucher> findByCode(String code) {
        return voucherRepository.findByCode(code);
    }

    @Transactional(readOnly = true)
    public List<Voucher> findActiveVouchers() {
        Date now = new Date();
        return voucherRepository.findByValidFromBeforeAndValidToAfterAndStatus(now, now, VoucherStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Page<Voucher> searchVouchers(VoucherSearchRequest request, Pageable pageable) {
        VoucherStatus status = request.getStatus();
        if (status == null && request.getActive() != null) {
            status = Boolean.TRUE.equals(request.getActive()) ? VoucherStatus.ACTIVE : VoucherStatus.EXPIRED;
        }
        return voucherRepository.searchVouchers(normalizeSearchText(request.getKeyword()), status, pageable);
    }

    public Voucher applyVoucher(String code) {
        return validateVoucher(code);
    }

    public Voucher updateVoucherStatus(String id, VoucherStatusUpdateRequest request) {
        accessControlService.requireOwnerRole();
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found: " + id));

        VoucherStatus nextStatus = request.getStatus();
        if (nextStatus == null && request.getActive() != null) {
            nextStatus = Boolean.TRUE.equals(request.getActive()) ? VoucherStatus.ACTIVE : VoucherStatus.EXPIRED;
        }
        if (nextStatus == null) {
            throw new IllegalArgumentException("Either status or active must be provided");
        }

        voucher.setStatus(nextStatus);
        return voucherRepository.save(voucher);
    }

    public Voucher disableVoucher(String id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found: " + id));
        voucher.setStatus(VoucherStatus.EXPIRED);
        return voucherRepository.save(voucher);
    }

    public Voucher consumeVoucher(String code) {
        Voucher voucher = validateVoucher(code);
        voucher.setUsageCount(voucher.getUsageCount() + 1);
        if (voucher.getQuantity() <= 0) {
            voucher.setQuantity(0);
            voucher.setStatus(VoucherStatus.USED_UP);
        }
        return voucherRepository.save(voucher);
    }

    private Voucher validateVoucher(String code) {
        Voucher voucher = voucherRepository.findByCode(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found: " + code));
        Date now = new Date();
        if (voucher.getStatus() != VoucherStatus.ACTIVE
                || now.before(voucher.getValidFrom())
                || now.after(voucher.getValidTo())) {
            throw new IllegalStateException("Voucher is not valid");
        }
        if (voucher.getUsageCount() >= voucher.getQuantity()) {
            voucher.setUsageCount(voucher.getQuantity());
            voucher.setStatus(VoucherStatus.USED_UP);
            voucherRepository.save(voucher);
            throw new IllegalStateException("Voucher has reached its usage limit");
        }
        return voucher;
    }

    private void validateVoucherCreate(VoucherCreateRequest request) {
        validateVoucherFields(request.getCode(), request.getDiscountPercent(), request.getValidFrom(), request.getValidTo(), request.getQuantity());
    }

    private void validateVoucherUpdate(VoucherUpdateRequest request) {
        validateVoucherFields(request.getCode(), request.getDiscountPercent(), request.getValidFrom(), request.getValidTo(), request.getQuantity());
    }

    private void validateVoucherFields(String code, Integer discountPercent, Date validFrom, Date validTo, Integer quantity) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Voucher code is required");
        }
        if (discountPercent == null || discountPercent <= 0) {
            throw new IllegalArgumentException("Discount value must be greater than 0");
        }
        if (validFrom == null || validTo == null || !validTo.after(validFrom)) {
            throw new IllegalArgumentException("End date must be after start date");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
    }

    private void applyVoucherRequest(Voucher voucher, VoucherCreateRequest request) {
        applyVoucherFields(voucher, request.getCode(), request.getDiscountPercent(), request.getValidFrom(), request.getValidTo(),
                request.getQuantity(), request.getStatus());
    }

    private void applyVoucherRequest(Voucher voucher, VoucherUpdateRequest request) {
        applyVoucherFields(voucher, request.getCode(), request.getDiscountPercent(), request.getValidFrom(), request.getValidTo(),
                request.getQuantity(), request.getStatus());
    }

    private void applyVoucherFields(Voucher voucher,
                                    String code,
                                    Integer discountPercent,
                                    Date validFrom,
                                    Date validTo,
                                    Integer quantity,
                                    VoucherStatus status) {
        voucher.setCode(code);
        voucher.setDiscountPercent(discountPercent);
        voucher.setValidFrom(validFrom);
        voucher.setValidTo(validTo);
        voucher.setQuantity(quantity);
        voucher.setStatus(status == null ? VoucherStatus.ACTIVE : status);
        if (voucher.getUsageCount() == null || voucher.getUsageCount() < 0) {
            voucher.setUsageCount(0);
        }
        if (voucher.getQuantity() == null || voucher.getQuantity() < 1) {
            voucher.setQuantity(1);
        }
    }

    private String normalizeSearchText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}





