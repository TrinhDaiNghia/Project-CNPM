package com.example.demo.controllers;

import com.example.demo.dtos.request.ChatRequest;
import com.example.demo.dtos.request.SupportReplyRequest;
import com.example.demo.dtos.response.ChatResponse;
import com.example.demo.dtos.response.ChatHistoryMessageResponse;
import com.example.demo.dtos.response.PageResponse;
import com.example.demo.dtos.response.SupportDiscussionResponse;
import com.example.demo.entities.Discuss;
import com.example.demo.services.AiChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AiChatService aiChatService;

    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> askBot(@Valid @RequestBody ChatRequest request) {
        String question = request.getMessage().trim();
        AiChatService.AiChatResult result = aiChatService.chatWithBot(question);

        ChatResponse response = ChatResponse.builder()
                .sender("System Bot")
                .message(result.message())
                .handledBy(result.handledBy())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/escalate")
    public ResponseEntity<ChatResponse> escalateToStaff() {
        AiChatService.AiChatResult result = aiChatService.escalateToStaff();

        ChatResponse response = ChatResponse.builder()
                .sender("System Bot")
                .message(result.message())
                .handledBy(result.handledBy())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatHistoryMessageResponse>> getMyHistory() {
        List<ChatHistoryMessageResponse> data = aiChatService.getMyChatHistory().stream()
                .map(message -> ChatHistoryMessageResponse.builder()
                        .id(message.id())
                        .role(message.role())
                        .content(message.content())
                        .createdAt(message.createdAt())
                        .handledBy(message.handledBy())
                        .build())
                .toList();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/support/pending")
    public ResponseEntity<List<SupportDiscussionResponse>> getPendingSupportDiscussions() {
        List<SupportDiscussionResponse> data = aiChatService.getOpenStaffSupportDiscussions().stream()
                .map(this::toSupportResponse)
                .toList();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/support/all")
    public ResponseEntity<PageResponse<SupportDiscussionResponse>> getAllSupportDiscussions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int pageSize,
            @RequestParam(defaultValue = "ALL") String status
    ) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        Pageable pageable = PageRequest.of(safePage - 1, safePageSize);

        Page<Discuss> result = aiChatService.getStaffSupportDiscussionsPage(status, pageable);
        List<SupportDiscussionResponse> items = result.getContent().stream()
                .map(this::toSupportResponse)
                .toList();
        PageResponse<SupportDiscussionResponse> response = PageResponse.<SupportDiscussionResponse>builder()
                .items(items)
                .page(safePage)
                .pageSize(safePageSize)
                .total(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/support/my-active")
    public ResponseEntity<SupportDiscussionResponse> getMyActiveSupportDiscussion() {
        SupportDiscussionResponse response = aiChatService.getMyOpenSupportDiscussion()
                .map(this::toSupportResponse)
                .orElse(null);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/support/{discussionId}/reply")
    public ResponseEntity<Void> replySupportDiscussion(
            @PathVariable String discussionId,
            @Valid @RequestBody SupportReplyRequest request
    ) {
        aiChatService.replySupportDiscussion(discussionId, request.getMessage().trim());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/support/{discussionId}/close")
    public ResponseEntity<Void> closeSupportDiscussion(@PathVariable String discussionId) {
        aiChatService.closeSupportDiscussion(discussionId);
        return ResponseEntity.noContent().build();
    }

    private SupportDiscussionResponse toSupportResponse(Discuss discuss) {
        String customerName = discuss.getCustomer() != null ? discuss.getCustomer().getFullName() : null;
        String customerId = discuss.getCustomer() != null ? discuss.getCustomer().getId() : null;
        return SupportDiscussionResponse.builder()
                .id(discuss.getId())
                .customerId(customerId)
                .customerName(customerName)
                .startDate(discuss.getStartDate())
                .endDate(discuss.getEndDate())
                .contentLog(aiChatService.buildDiscussionContentLog(discuss))
                .aiHandled(discuss.getIsAiHandled())
                .build();
    }
}
