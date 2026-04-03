package com.example.demo.repositories;

import com.example.demo.entities.Discuss;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscussRepository extends JpaRepository<Discuss, String> {

    Optional<Discuss> findFirstByCustomerIdAndIsAiHandledFalseAndEndDateIsNullOrderByStartDateDesc(String customerId);
    List<Discuss> findByCustomerIdOrderByStartDateAsc(String customerId);

    @Query("SELECT d FROM Discuss d JOIN FETCH d.customer WHERE d.isAiHandled = false AND d.endDate IS NULL ORDER BY d.startDate DESC")
    List<Discuss> findOpenSupportDiscussionsWithCustomer();

    @Query("SELECT d FROM Discuss d JOIN FETCH d.customer WHERE d.isAiHandled = false ORDER BY d.startDate DESC")
    List<Discuss> findAllSupportDiscussionsWithCustomer();

    @Query("SELECT d FROM Discuss d JOIN FETCH d.customer WHERE d.customer.id = :customerId AND d.isAiHandled = false AND d.endDate IS NULL ORDER BY d.startDate DESC")
    List<Discuss> findOpenSupportByCustomerIdWithCustomer(@Param("customerId") String customerId);
}
