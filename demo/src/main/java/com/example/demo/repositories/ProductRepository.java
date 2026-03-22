package com.example.demo.repositories;

import com.example.demo.entities.Product;
import com.example.demo.entities.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByCategoryId(String categoryId);

    List<Product> findByStatus(ProductStatus status);

    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.stockQuantity > 0 AND p.status = 'ACTIVE'")
    List<Product> findAvailableProducts();

    @Query("SELECT p FROM Product p WHERE p.brand = :brand AND p.status = :status")
    List<Product> findByBrandAndStatus(@Param("brand") String brand, @Param("status") ProductStatus status);

    boolean existsByNameAndCategoryId(String name, String categoryId);
}
