package com.example.demo.services;

import com.example.demo.dtos.request.VoucherRequest;
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
    public Voucher createVoucher(VoucherRequest request) {
        Voucher voucher = new Voucher();
        applyVoucherRequest(voucher, request);
        return voucherRepository.save(voucher);
    }

    public Voucher updateVoucher(String id, VoucherRequest request) {
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
        return voucherRepository.findByVoucherCode(code);
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
        voucher.setUsedCount(voucher.getUsedCount() + 1);
        if (voucher.getUsedCount() >= voucher.getMaxUsage()) {
            voucher.setStatus(VoucherStatus.USED_UP);
        }
        return voucherRepository.save(voucher);
    }

    private Voucher validateVoucher(String code) {
        Voucher voucher = voucherRepository.findByVoucherCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found: " + code));
        Date now = new Date();
        if (voucher.getStatus() != VoucherStatus.ACTIVE
                || now.before(voucher.getValidFrom())
                || now.after(voucher.getValidTo())) {
            throw new IllegalStateException("Voucher is not valid");
        }
        if (voucher.getUsedCount() >= voucher.getMaxUsage()) {
            if (voucher.getStatus() != VoucherStatus.USED_UP) {
                voucher.setStatus(VoucherStatus.USED_UP);
                voucherRepository.save(voucher);
            }
            throw new IllegalStateException("Voucher has reached its usage limit");
        }
        return voucher;
    }

    private void applyVoucherRequest(Voucher voucher, VoucherRequest request) {
        voucher.setVoucherCode(request.getVoucherCode());
        voucher.setDiscountPercent(request.getDiscountPercent());
        voucher.setMinOrderAmount(request.getMinOrderAmount());
        voucher.setValidFrom(request.getValidFrom());
        voucher.setValidTo(request.getValidTo());
        voucher.setMaxUsage(request.getMaxUsage());
        if (voucher.getStatus() == null) {
            voucher.setStatus(VoucherStatus.ACTIVE);
        }
        if (voucher.getUsedCount() == null) {
            voucher.setUsedCount(0);
        }
    }
}





