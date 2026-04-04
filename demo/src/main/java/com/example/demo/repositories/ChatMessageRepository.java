package com.example.demo.repositories;

import com.example.demo.entities.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    List<ChatMessage> findByChatIdOrderByCreatedAtAsc(String chatId);

    List<ChatMessage> findByChatCustomerIdOrderByCreatedAtAsc(String customerId);

    List<ChatMessage> findByChatCustomerIdAndChatIsAiHandledTrueOrderByCreatedAtDesc(
            String customerId,
            Pageable pageable
    );
}

