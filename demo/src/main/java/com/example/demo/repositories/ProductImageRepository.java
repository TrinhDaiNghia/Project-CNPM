package com.example.demo.repositories;

import com.example.demo.entities.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, String> {

    Optional<ProductImage> findByIdAndProductId(String id, String productId);

    List<ProductImage> findByProductId(String productId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ProductImage pi SET pi.isThumbnail = false WHERE pi.product.id = :productId")
    int clearThumbnailByProductId(@Param("productId") String productId);
}

