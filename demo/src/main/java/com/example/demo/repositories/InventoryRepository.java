package com.example.demo.repositories;

import com.example.demo.entities.Inventory;
import com.example.demo.entities.enums.InventoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, String> {

    List<Inventory> findByStatus(InventoryStatus status);

    List<Inventory> findByOwnerId(String ownerId);

    List<Inventory> findByCustomerId(String customerId);

    List<Inventory> findByProductId(String productId);
}
