package com.example.demo.services;

import com.example.demo.entities.Voucher;

import java.util.List;
import java.util.Optional;

public interface VoucherService {

    Voucher createVoucher(Voucher voucher);

    Voucher updateVoucher(String id, Voucher voucher);

    void deleteVoucher(String id);

    Optional<Voucher> findById(String id);

    Optional<Voucher> findByCode(String code);

    List<Voucher> findActiveVouchers();

    Voucher applyVoucher(String code);
}
