package com.example.demo.repositories;

import com.example.demo.entities.DiscussionMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscussionMessageRepository extends JpaRepository<DiscussionMessage, String> {

    List<DiscussionMessage> findByDiscussIdOrderByCreatedAtAsc(String discussionId);

    List<DiscussionMessage> findByDiscussCustomerIdOrderByCreatedAtAsc(String customerId);

    List<DiscussionMessage> findByDiscussCustomerIdAndDiscussIsAiHandledTrueOrderByCreatedAtDesc(
            String customerId,
            Pageable pageable
    );
}

