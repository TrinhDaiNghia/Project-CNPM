package com.example.demo.repositories;

import com.example.demo.entities.Voucher;
import com.example.demo.entities.enums.VoucherStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, String> {

    Optional<Voucher> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, String id);

    List<Voucher> findByStatus(VoucherStatus status);

    List<Voucher> findByValidFromBeforeAndValidToAfterAndStatus(Date now1, Date now2, VoucherStatus status);

    @Query("SELECT v FROM Voucher v " +
            "WHERE (:keyword IS NULL OR LOWER(v.code) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR v.status = :status)")
    Page<Voucher> searchVouchers(@Param("keyword") String keyword,
                                 @Param("status") VoucherStatus status,
                                 Pageable pageable);

    @Query("SELECT v FROM Voucher v " +
            "WHERE :atTime BETWEEN v.validFrom AND v.validTo " +
            "AND v.status = :status")
    List<Voucher> findValidVouchersAt(@Param("atTime") Date atTime, @Param("status") VoucherStatus status);

    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Order o WHERE o.voucher.id = :voucherId")
    boolean existsUsedInOrders(@Param("voucherId") String voucherId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Voucher v SET v.status = :status WHERE v.id = :voucherId")
    int updateStatus(@Param("voucherId") String voucherId, @Param("status") VoucherStatus status);
}
