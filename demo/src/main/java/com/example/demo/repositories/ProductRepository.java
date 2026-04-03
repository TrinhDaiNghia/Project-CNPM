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

    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN p.categories c WHERE p.category.id = :categoryId OR c.id = :categoryId")
    List<Product> findByAnyCategoryId(@Param("categoryId") String categoryId);

    @Query("SELECT CASE WHEN COUNT(DISTINCT p) > 0 THEN true ELSE false END FROM Product p LEFT JOIN p.categories c WHERE p.category.id = :categoryId OR c.id = :categoryId")
    boolean existsByAnyCategoryId(@Param("categoryId") String categoryId);

    List<Product> findByStatus(ProductStatus status);

    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("SELECT p FROM Product p " +
            "WHERE (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:brand IS NULL OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :brand, '%'))) " +
            "AND (:color IS NULL OR LOWER(COALESCE(p.wireColor, '')) LIKE LOWER(CONCAT('%', :color, '%')) " +
            "OR LOWER(COALESCE(p.caseColor, '')) LIKE LOWER(CONCAT('%', :color, '%')) " +
            "OR LOWER(COALESCE(p.faceColor, '')) LIKE LOWER(CONCAT('%', :color, '%'))) " +
            "AND (:faceSize IS NULL OR LOWER(COALESCE(p.faceSize, '')) LIKE LOWER(CONCAT('%', :faceSize, '%'))) " +
            "AND (:spec IS NULL OR LOWER(COALESCE(p.movementType, '')) LIKE LOWER(CONCAT('%', :spec, '%')) " +
            "OR LOWER(COALESCE(p.glassMaterial, '')) LIKE LOWER(CONCAT('%', :spec, '%')) " +
            "OR LOWER(COALESCE(p.waterResistance, '')) LIKE LOWER(CONCAT('%', :spec, '%')) " +
            "OR LOWER(COALESCE(p.wireMaterial, '')) LIKE LOWER(CONCAT('%', :spec, '%')) " +
            "OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :spec, '%'))) " +
            "AND (:status IS NULL OR p.status = :status)")
    Page<Product> searchProducts(@Param("name") String name,
                                 @Param("brand") String brand,
                                 @Param("color") String color,
                                 @Param("faceSize") String faceSize,
                                 @Param("spec") String spec,
                                 @Param("status") ProductStatus status,
                                 Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.stockQuantity > 0 AND p.status = 'ACTIVE'")
    List<Product> findAvailableProducts();

    @Query("SELECT p FROM Product p WHERE p.brand = :brand AND p.status = :status")
    List<Product> findByBrandAndStatus(@Param("brand") String brand, @Param("status") ProductStatus status);

    @Query("SELECT CASE WHEN COUNT(DISTINCT p) > 0 THEN true ELSE false END FROM Product p LEFT JOIN p.categories c WHERE LOWER(p.name) = LOWER(:name) AND (p.category.id = :categoryId OR c.id = :categoryId)")
    boolean existsByNameAndAnyCategoryId(@Param("name") String name, @Param("categoryId") String categoryId);

    @Query("SELECT CASE WHEN COUNT(DISTINCT p) > 0 THEN true ELSE false END FROM Product p LEFT JOIN p.categories c WHERE LOWER(p.name) = LOWER(:name) AND (p.category.id = :categoryId OR c.id = :categoryId) AND p.id <> :id")
    boolean existsByNameAndAnyCategoryIdAndIdNot(@Param("name") String name, @Param("categoryId") String categoryId, @Param("id") String id);

    @Query("SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END FROM OrderItem oi WHERE oi.product.id = :productId")
    boolean existsRelatedTransactions(@Param("productId") String productId);
}
