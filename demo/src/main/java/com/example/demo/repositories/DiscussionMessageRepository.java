package com.example.demo.repositories;

import com.example.demo.entities.DiscussionMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscussionMessageRepository extends JpaRepository<DiscussionMessage, String> {

    List<DiscussionMessage> findByProductIdOrderByCreatedAtAsc(String productId);

    List<DiscussionMessage> findTop12ByProductIdAndUserIdOrderByCreatedAtDesc(String productId, String userId);
}

