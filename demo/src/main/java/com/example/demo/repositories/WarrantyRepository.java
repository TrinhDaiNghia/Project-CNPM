package com.example.demo.repositories;

import com.example.demo.entities.Warranty;
import com.example.demo.entities.enums.WarrantyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarrantyRepository extends JpaRepository<Warranty, String> {

    List<Warranty> findByCustomerId(String customerId);

    List<Warranty> findByStatus(WarrantyStatus status);

    List<Warranty> findByCustomerIdAndStatus(String customerId, WarrantyStatus status);

    boolean existsByCustomerIdAndOrderIdAndProductIdAndStatus(
            String customerId,
            String orderId,
            String productId,
            WarrantyStatus status
    );
}
