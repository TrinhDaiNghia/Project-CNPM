package com.example.demo.services.impl;

import com.example.demo.entities.Voucher;
import com.example.demo.entities.enums.VoucherStatus;
import com.example.demo.repositories.VoucherRepository;
import com.example.demo.services.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;

    @Override
    public Voucher createVoucher(Voucher voucher) {
        return voucherRepository.save(voucher);
    }

    @Override
    public Voucher updateVoucher(String id, Voucher voucher) {
        voucher.setId(id);
        return voucherRepository.save(voucher);
    }

    @Override
    public void deleteVoucher(String id) {
        voucherRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Voucher> findById(String id) {
        return voucherRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Voucher> findByCode(String code) {
        return voucherRepository.findByVoucherCode(code);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Voucher> findActiveVouchers() {
        Date now = new Date();
        return voucherRepository.findByValidFromBeforeAndValidToAfterAndStatus(now, now, VoucherStatus.ACTIVE);
    }

    @Override
    public Voucher applyVoucher(String code) {
        Voucher voucher = voucherRepository.findByVoucherCode(code)
                .orElseThrow(() -> new RuntimeException("Voucher not found: " + code));
        Date now = new Date();
        if (voucher.getStatus() != VoucherStatus.ACTIVE
                || now.before(voucher.getValidFrom())
                || now.after(voucher.getValidTo())) {
            throw new IllegalStateException("Voucher is not valid");
        }
        if (voucher.getUsedCount() >= voucher.getMaxUsage()) {
            voucher.setStatus(VoucherStatus.USED_UP);
            voucherRepository.save(voucher);
            throw new IllegalStateException("Voucher has reached its usage limit");
        }
        voucher.setUsedCount(voucher.getUsedCount() + 1);
        return voucherRepository.save(voucher);
    }
}
