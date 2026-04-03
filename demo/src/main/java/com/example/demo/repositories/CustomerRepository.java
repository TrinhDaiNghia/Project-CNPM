package com.example.demo.repositories;

import com.example.demo.entities.Customer;
import com.example.demo.entities.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

    Optional<Customer> findByUsername(String username);

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByPhone(String phone);

    default Optional<Customer> findByUserUsername(String username) {
        return findByUsername(username);
    }

    default Optional<Customer> findByUserEmail(String email) {
        return findByEmail(email);
    }

    @Query("SELECT c FROM Customer c " +
            "WHERE c.role = :role " +
            "AND (:fullName IS NULL OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :fullName, '%'))) " +
            "AND (:email IS NULL OR LOWER(c.email) LIKE LOWER(CONCAT('%', :email, '%'))) " +
            "AND (:phone IS NULL OR LOWER(COALESCE(c.phone, '')) LIKE LOWER(CONCAT('%', :phone, '%'))) " +
            "AND (:address IS NULL OR LOWER(COALESCE(c.address, '')) LIKE LOWER(CONCAT('%', :address, '%')))")
    Page<Customer> searchCustomers(@Param("role") UserRole role,
                                   @Param("fullName") String fullName,
                                   @Param("email") String email,
                                   @Param("phone") String phone,
                                   @Param("address") String address,
                                   Pageable pageable);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmailAndIdNot(String email, String id);

    boolean existsByPhoneAndIdNot(String phone, String id);

    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Order o WHERE o.customer.id = :customerId")
    boolean existsRelatedOrders(@Param("customerId") String customerId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Review r WHERE r.customer.id = :customerId")
    boolean existsRelatedReviews(@Param("customerId") String customerId);

    @Modifying
    @Query(value = "DELETE FROM customers WHERE id = :customerId", nativeQuery = true)
    int deleteCustomerProfileById(@Param("customerId") String customerId);

    @Query("SELECT COUNT(c) FROM Customer c " +
            "WHERE (:startDate IS NULL OR c.createdAt >= :startDate) " +
            "AND (:endDate IS NULL OR c.createdAt <= :endDate)")
    long countNewCustomersBetween(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
}
