package com.example.demo.repositories;

import com.example.demo.entities.ImportReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportReceiptRepository extends JpaRepository<ImportReceipt, String> {

    List<ImportReceipt> findBySupplierId(String supplierId);

    List<ImportReceipt> findByOwnerId(String ownerId);

    List<ImportReceipt> findByOwnerIdAndSupplierId(String ownerId, String supplierId);
}
