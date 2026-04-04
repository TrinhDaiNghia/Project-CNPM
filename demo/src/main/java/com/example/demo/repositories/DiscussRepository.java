package com.example.demo.repositories;

import com.example.demo.entities.Discuss;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscussRepository extends JpaRepository<Discuss, String> {

    Optional<Discuss> findFirstByCustomerIdAndIsAiHandledFalseAndEndDateIsNullOrderByStartDateDesc(String customerId);

    @Query(
            value = "SELECT d FROM Discuss d JOIN FETCH d.customer c WHERE d.isAiHandled = false ORDER BY d.startDate DESC",
            countQuery = "SELECT COUNT(d) FROM Discuss d JOIN d.customer c WHERE d.isAiHandled = false"
    )
    Page<Discuss> findAllSupportDiscussionsPageWithCustomer(Pageable pageable);

    @Query(
            value = "SELECT d FROM Discuss d JOIN FETCH d.customer c WHERE d.isAiHandled = false AND d.endDate IS NULL ORDER BY d.startDate DESC",
            countQuery = "SELECT COUNT(d) FROM Discuss d JOIN d.customer c WHERE d.isAiHandled = false AND d.endDate IS NULL"
    )
    Page<Discuss> findOpenSupportDiscussionsPageWithCustomer(Pageable pageable);

    @Query(
            value = "SELECT d FROM Discuss d JOIN FETCH d.customer c WHERE d.isAiHandled = false AND d.endDate IS NOT NULL ORDER BY d.startDate DESC",
            countQuery = "SELECT COUNT(d) FROM Discuss d JOIN d.customer c WHERE d.isAiHandled = false AND d.endDate IS NOT NULL"
    )
    Page<Discuss> findClosedSupportDiscussionsPageWithCustomer(Pageable pageable);

    @Query("SELECT d FROM Discuss d JOIN FETCH d.customer WHERE d.isAiHandled = false AND d.endDate IS NULL ORDER BY d.startDate DESC")
    List<Discuss> findOpenSupportDiscussionsWithCustomer();

    @Query("SELECT d FROM Discuss d JOIN FETCH d.customer WHERE d.isAiHandled = false ORDER BY d.startDate DESC")
    List<Discuss> findAllSupportDiscussionsWithCustomer();
}
