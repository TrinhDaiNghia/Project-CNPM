package com.example.demo.services;

import com.example.demo.dtos.request.VoucherCreateRequest;
import com.example.demo.dtos.request.VoucherUpdateRequest;
import com.example.demo.entities.Voucher;
import com.example.demo.entities.enums.VoucherStatus;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.VoucherRepository;
import lombok.RequiredArgsConstructor;
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
    public Voucher createVoucher(VoucherCreateRequest request) {
        Voucher voucher = new Voucher();
        applyVoucherRequest(voucher, request);
        return voucherRepository.save(voucher);
    }

    public Voucher updateVoucher(String id, VoucherUpdateRequest request) {
        Voucher existing = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found: " + id));
        applyVoucherRequest(existing, request);
        return voucherRepository.save(existing);
    }

    public void deleteVoucher(String id) {
        voucherRepository.deleteById(id);
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
    public Voucher applyVoucher(String code) {
        return validateVoucher(code);
    }

    public Voucher consumeVoucher(String code) {
        Voucher voucher = validateVoucher(code);
        voucher.setQuantity(voucher.getQuantity() - 1);
        voucher.setUsedAt(new Date());
        if (voucher.getQuantity() <= 0) {
            voucher.setQuantity(0);
            voucher.setIsUsed(true);
            voucher.setStatus(VoucherStatus.USED_UP);
        }
        return voucherRepository.save(voucher);
    }

    private Voucher validateVoucher(String code) {
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found: " + code));
        Date now = new Date();
        if (voucher.getStatus() != VoucherStatus.ACTIVE
                || now.before(voucher.getValidFrom())
                || now.after(voucher.getValidTo())) {
            throw new IllegalStateException("Voucher is not valid");
        }
        if (Boolean.TRUE.equals(voucher.getIsUsed()) || voucher.getQuantity() <= 0) {
            voucher.setIsUsed(true);
            voucher.setQuantity(0);
            voucher.setStatus(VoucherStatus.USED_UP);
            voucherRepository.save(voucher);
            throw new IllegalStateException("Voucher has reached its usage limit");
        }
        return voucher;
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
        if (voucher.getIsUsed() == null) {
            voucher.setIsUsed(false);
        }
        if (voucher.getQuantity() == null || voucher.getQuantity() < 1) {
            voucher.setQuantity(1);
        }
    }
}





