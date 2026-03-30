package com.example.demo.repositories;

import com.example.demo.entities.Order;
import com.example.demo.entities.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findByCustomerId(String customerId);

    Page<Order> findByCustomerId(String customerId, Pageable pageable);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByCustomerIdAndStatus(String customerId, OrderStatus status);

    boolean existsByCustomerId(String customerId);

    boolean existsByVoucherId(String voucherId);

    @Query("SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END FROM OrderItem oi WHERE oi.product.id = :productId")
    boolean existsByProductId(@Param("productId") String productId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
            "WHERE (:startDate IS NULL OR o.orderDate >= :startDate) " +
            "AND (:endDate IS NULL OR o.orderDate <= :endDate)")
    Long sumRevenueBetween(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE (:startDate IS NULL OR o.orderDate >= :startDate) " +
            "AND (:endDate IS NULL OR o.orderDate <= :endDate)")
    long countOrdersBetween(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi " +
            "JOIN oi.order o " +
            "WHERE (:startDate IS NULL OR o.orderDate >= :startDate) " +
            "AND (:endDate IS NULL OR o.orderDate <= :endDate)")
    Long sumSoldQuantityBetween(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    @Query("SELECT FUNCTION('DATE', o.orderDate), COUNT(o) FROM Order o " +
            "WHERE (:startDate IS NULL OR o.orderDate >= :startDate) " +
            "AND (:endDate IS NULL OR o.orderDate <= :endDate) " +
            "GROUP BY FUNCTION('DATE', o.orderDate) " +
            "ORDER BY FUNCTION('DATE', o.orderDate)")
    List<Object[]> countOrdersByDay(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    @Query("SELECT FUNCTION('DATE_FORMAT', o.orderDate, '%Y-%m'), COUNT(o) FROM Order o " +
            "WHERE (:startDate IS NULL OR o.orderDate >= :startDate) " +
            "AND (:endDate IS NULL OR o.orderDate <= :endDate) " +
            "GROUP BY FUNCTION('DATE_FORMAT', o.orderDate, '%Y-%m') " +
            "ORDER BY FUNCTION('DATE_FORMAT', o.orderDate, '%Y-%m')")
    List<Object[]> countOrdersByMonth(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    @Query("SELECT FUNCTION('DATE', o.orderDate), COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
            "WHERE (:startDate IS NULL OR o.orderDate >= :startDate) " +
            "AND (:endDate IS NULL OR o.orderDate <= :endDate) " +
            "GROUP BY FUNCTION('DATE', o.orderDate) " +
            "ORDER BY FUNCTION('DATE', o.orderDate)")
    List<Object[]> sumRevenueByDay(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    @Query("SELECT oi.product.id, oi.product.name, SUM(oi.quantity) AS soldQuantity " +
            "FROM OrderItem oi JOIN oi.order o " +
            "WHERE (:startDate IS NULL OR o.orderDate >= :startDate) " +
            "AND (:endDate IS NULL OR o.orderDate <= :endDate) " +
            "GROUP BY oi.product.id, oi.product.name " +
            "ORDER BY soldQuantity DESC")
    Page<Object[]> findTopSellingProducts(@Param("startDate") Date startDate,
                                          @Param("endDate") Date endDate,
                                          Pageable pageable);
}
