package com.example.demo.repositories;

import com.example.demo.entities.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, String> {

    Optional<Supplier> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, String id);

    @Query("SELECT s FROM Supplier s " +
            "WHERE (:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(COALESCE(s.contractInfo, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(COALESCE(s.address, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:contractInfo IS NULL OR LOWER(COALESCE(s.contractInfo, '')) LIKE LOWER(CONCAT('%', :contractInfo, '%'))) " +
            "AND (:address IS NULL OR LOWER(COALESCE(s.address, '')) LIKE LOWER(CONCAT('%', :address, '%')))")
    Page<Supplier> searchSuppliers(@Param("keyword") String keyword,
                                   @Param("name") String name,
                                   @Param("contractInfo") String contractInfo,
                                   @Param("address") String address,
                                   Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(ir) > 0 THEN true ELSE false END FROM ImportReceipt ir WHERE ir.supplier.id = :supplierId")
    boolean existsRelatedRecords(@Param("supplierId") String supplierId);
}
