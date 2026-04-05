package com.example.demo.repositories;

import com.example.demo.entities.DiscussionMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscussionMessageRepository extends JpaRepository<DiscussionMessage, String> {

    Page<DiscussionMessage> findByProductIdAndParentIdIsNullOrderByCreatedAtDesc(String productId, Pageable pageable);

    List<DiscussionMessage> findByProductIdAndParentIdInOrderByCreatedAtAsc(String productId, List<String> parentIds);

    List<DiscussionMessage> findTop12ByProductIdAndUserIdOrderByCreatedAtDesc(String productId, String userId);
}
