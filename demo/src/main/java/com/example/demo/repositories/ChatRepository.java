package com.example.demo.repositories;

import com.example.demo.entities.Chat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, String> {

    Optional<Chat> findFirstByCustomerIdAndIsAiHandledFalseAndEndDateIsNullOrderByStartDateDesc(String customerId);

    @Query(
            value = "SELECT c FROM Chat c JOIN FETCH c.customer cu WHERE c.isAiHandled = false ORDER BY c.startDate DESC",
            countQuery = "SELECT COUNT(c) FROM Chat c JOIN c.customer cu WHERE c.isAiHandled = false"
    )
    Page<Chat> findAllSupportChatsPageWithCustomer(Pageable pageable);

    @Query(
            value = "SELECT c FROM Chat c JOIN FETCH c.customer cu WHERE c.isAiHandled = false AND c.endDate IS NULL ORDER BY c.startDate DESC",
            countQuery = "SELECT COUNT(c) FROM Chat c JOIN c.customer cu WHERE c.isAiHandled = false AND c.endDate IS NULL"
    )
    Page<Chat> findOpenSupportChatsPageWithCustomer(Pageable pageable);

    @Query(
            value = "SELECT c FROM Chat c JOIN FETCH c.customer cu WHERE c.isAiHandled = false AND c.endDate IS NOT NULL ORDER BY c.startDate DESC",
            countQuery = "SELECT COUNT(c) FROM Chat c JOIN c.customer cu WHERE c.isAiHandled = false AND c.endDate IS NOT NULL"
    )
    Page<Chat> findClosedSupportChatsPageWithCustomer(Pageable pageable);

    @Query("SELECT c FROM Chat c JOIN FETCH c.customer WHERE c.isAiHandled = false AND c.endDate IS NULL ORDER BY c.startDate DESC")
    List<Chat> findOpenSupportChatsWithCustomer();

    @Query("SELECT c FROM Chat c JOIN FETCH c.customer WHERE c.isAiHandled = false ORDER BY c.startDate DESC")
    List<Chat> findAllSupportChatsWithCustomer();
}
