package com.example.demo.repositories;

import com.example.demo.entities.Voucher;
import com.example.demo.entities.enums.VoucherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, String> {

    Optional<Voucher> findByVoucherCode(String voucherCode);

    List<Voucher> findByStatus(VoucherStatus status);

    List<Voucher> findByValidFromBeforeAndValidToAfterAndStatus(Date now1, Date now2, VoucherStatus status);
}
