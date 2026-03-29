package com.example.demo.repositories;

import com.example.demo.entities.Warranty;
import com.example.demo.entities.enums.WarrantyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarrantyRepository extends JpaRepository<Warranty, String> {

    Page<Warranty> findByStatus(WarrantyStatus status, Pageable pageable);

    @Query("SELECT w FROM Warranty w " +
            "WHERE (:keyword IS NULL OR LOWER(w.customerName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(w.customerPhone) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(COALESCE(w.issueDescription, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR w.status = :status)")
    Page<Warranty> searchWarrantyRequests(@Param("keyword") String keyword,
                                          @Param("status") WarrantyStatus status,
                                          Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Warranty w SET w.status = :status WHERE w.id = :id")
    int updateStatus(@Param("id") String id, @Param("status") WarrantyStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Warranty w SET w.status = :status, w.rejectReason = :rejectReason WHERE w.id = :id")
    int updateStatusAndRejectReason(@Param("id") String id,
                                    @Param("status") WarrantyStatus status,
                                    @Param("rejectReason") String rejectReason);

    List<Warranty> findByStatus(WarrantyStatus status);
}
