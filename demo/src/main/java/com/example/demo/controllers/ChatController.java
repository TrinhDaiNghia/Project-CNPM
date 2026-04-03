package com.example.demo.controllers;

import com.example.demo.dtos.request.ChatRequest;
import com.example.demo.dtos.response.ChatResponse;
import com.example.demo.services.AiChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AiChatService aiChatService;

    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> askBot(@Valid @RequestBody ChatRequest request) {
        String question = request.getMessage().trim();
        String answer = aiChatService.chatWithBot(question);

        ChatResponse response = ChatResponse.builder()
                .sender("System Bot")
                .message(answer)
                .handledBy("AI")
                .build();

        return ResponseEntity.ok(response);
    }
}

