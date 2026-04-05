package com.example.demo.repositories;

import com.example.demo.entities.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, String> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findByCustomerId(String customerId);

    boolean existsByCustomerId(String customerId);

    @Modifying
    @Query(value = "DELETE FROM cart_items WHERE cart_id IN (SELECT id FROM carts WHERE customer_id = :customerId)", nativeQuery = true)
    int deleteItemsByCustomerId(@Param("customerId") String customerId);

    @Modifying
    @Query(value = "DELETE FROM carts WHERE customer_id = :customerId", nativeQuery = true)
    int deleteByCustomerId(@Param("customerId") String customerId);
}
