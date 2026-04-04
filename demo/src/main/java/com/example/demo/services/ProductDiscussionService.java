package com.example.demo.services;

import com.example.demo.dtos.response.PageResponse;
import com.example.demo.dtos.response.ProductDiscussionAskResponse;
import com.example.demo.dtos.response.ProductDiscussionMessageResponse;
import com.example.demo.entities.DiscussionMessage;
import com.example.demo.entities.Product;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.DiscussionHandledBy;
import com.example.demo.entities.enums.DiscussionMessageRole;
import com.example.demo.entities.enums.UserRole;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.DiscussionMessageRepository;
import com.example.demo.repositories.ProductRepository;
import com.example.demo.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductDiscussionService {

    private final DiscussionMessageRepository discussionMessageRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final GeminiService geminiService;
    private final AccessControlService accessControlService;

    @Transactional(readOnly = true)
    public PageResponse<ProductDiscussionMessageResponse> listByProduct(String productId, int page, int pageSize) {
        ensureProductExists(productId);
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        Pageable pageable = PageRequest.of(safePage - 1, safePageSize);

        // Phân trang theo câu hỏi gốc (mới nhất trước), không phân trang theo từng message.
        Page<DiscussionMessage> questionPage =
                discussionMessageRepository.findByProductIdAndParentIdIsNullOrderByCreatedAtDesc(productId, pageable);

        List<DiscussionMessage> orderedMessages = buildOrderedMessages(productId, questionPage.getContent());
        Map<String, String> senderNames = resolveSenderNames(orderedMessages);

        List<ProductDiscussionMessageResponse> items = orderedMessages.stream()
                .map(message -> toResponse(message, senderNames))
                .toList();

        return PageResponse.<ProductDiscussionMessageResponse>builder()
                .items(items)
                .page(safePage)
                .pageSize(safePageSize)
                .total(questionPage.getTotalElements())
                .totalPages(questionPage.getTotalPages())
                .build();
    }

    @Transactional
    public ProductDiscussionAskResponse ask(String productId, String question) {
        if (!StringUtils.hasText(question)) {
            throw new IllegalArgumentException("Question is required");
        }
        String normalizedQuestion = question.trim();

        Product product = ensureProductExists(productId);
        User currentUser = accessControlService.getCurrentUserOrThrow();
        if (currentUser.getRole() != UserRole.CUSTOMER) {
            throw new IllegalStateException("Only CUSTOMER can send product discussion questions");
        }

        List<Message> history = buildRecentHistory(productId, currentUser.getId());

        DiscussionMessage questionMessage = discussionMessageRepository.saveAndFlush(
                DiscussionMessage.builder()
                        .productId(productId)
                        .userId(currentUser.getId())
                        .content(normalizedQuestion)
                        .role(DiscussionMessageRole.USER)
                        .handledBy(DiscussionHandledBy.CUSTOMER)
                        .aiHandled(false)
                        .build()
        );

        // THẢO LUẬN sản phẩm: chỉ dùng dữ liệu trực tiếp từ bảng product.
        String context = buildProductContext(product);

        String answer = geminiService.generateDiscussionAnswer(context, normalizedQuestion, history);
        if (!StringUtils.hasText(answer)) {
            answer = "Xin lỗi, hiện tại hệ thống chưa thể trả lời câu hỏi này. Vui lòng thử lại sau.";
        }

        DiscussionMessage answerMessage = discussionMessageRepository.saveAndFlush(
                DiscussionMessage.builder()
                        .productId(productId)
                        .userId(currentUser.getId())
                        .content(answer)
                        .parentId(questionMessage.getId())
                        .role(DiscussionMessageRole.ASSISTANT)
                        .handledBy(DiscussionHandledBy.AI)
                        .aiHandled(true)
                        .build()
        );

        Map<String, String> askSenderNames = Map.of(
                currentUser.getId(),
                StringUtils.hasText(currentUser.getFullName()) ? currentUser.getFullName().trim() : currentUser.getUsername()
        );

        return ProductDiscussionAskResponse.builder()
                .question(toResponse(questionMessage, askSenderNames))
                .answer(toResponse(answerMessage, askSenderNames))
                .build();
    }

    private Product ensureProductExists(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
    }

    private List<Message> buildRecentHistory(String productId, String userId) {
        try {
            List<DiscussionMessage> latestRows =
                    discussionMessageRepository.findTop12ByProductIdAndUserIdOrderByCreatedAtDesc(productId, userId);
            if (latestRows.isEmpty()) {
                return List.of();
            }

            Collections.reverse(latestRows);
            List<Message> history = new ArrayList<>();
            for (DiscussionMessage row : latestRows) {
                if (!StringUtils.hasText(row.getContent())) {
                    continue;
                }
                if (row.getRole() == DiscussionMessageRole.USER) {
                    history.add(new UserMessage(row.getContent()));
                } else if (row.getRole() == DiscussionMessageRole.ASSISTANT) {
                    history.add(new AssistantMessage(row.getContent()));
                }
            }
            return history;
        } catch (Exception ex) {
            log.warn("Failed to build product discussion history for productId={}", productId, ex);
            return List.of();
        }
    }

    private String buildProductContext(Product product) {
        return String.format(
                "Sản phẩm: %s. Thương hiệu: %s. Giá bán: %s VND. Số lượng tồn kho: %d. Loại máy: %s. Chất liệu kính: %s. " +
                        "Kháng nước: %s. Kích thước mặt: %s. Chất liệu dây: %s. Màu dây: %s. Màu vỏ: %s. " +
                        "Màu mặt: %s. Mô tả: %s.",
                safeText(product.getName()),
                safeText(product.getBrand()),
                formatPrice(product.getPrice()),
                product.getStockQuantity() == null ? 0 : product.getStockQuantity(),
                safeText(product.getMovementType()),
                safeText(product.getGlassMaterial()),
                safeText(product.getWaterResistance()),
                safeText(product.getFaceSize()),
                safeText(product.getWireMaterial()),
                safeText(product.getWireColor()),
                safeText(product.getCaseColor()),
                safeText(product.getFaceColor()),
                safeText(product.getDescription())
        );
    }

    private String safeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "không rõ";
    }

    private String formatPrice(Long price) {
        if (price == null) {
            return "không rõ";
        }
        return String.format("%,d", price);
    }

    private List<DiscussionMessage> buildOrderedMessages(String productId, List<DiscussionMessage> questions) {
        if (questions == null || questions.isEmpty()) {
            return List.of();
        }

        List<String> questionIds = questions.stream()
                .map(DiscussionMessage::getId)
                .filter(StringUtils::hasText)
                .toList();
        if (questionIds.isEmpty()) {
            return questions;
        }

        List<DiscussionMessage> answers =
                discussionMessageRepository.findByProductIdAndParentIdInOrderByCreatedAtAsc(productId, questionIds);
        Map<String, List<DiscussionMessage>> answersByParent = answers.stream()
                .collect(Collectors.groupingBy(DiscussionMessage::getParentId, LinkedHashMap::new, Collectors.toList()));

        List<DiscussionMessage> ordered = new ArrayList<>();
        for (DiscussionMessage question : questions) {
            ordered.add(question);
            ordered.addAll(answersByParent.getOrDefault(question.getId(), List.of()));
        }
        return ordered;
    }

    private Map<String, String> resolveSenderNames(List<DiscussionMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return Map.of();
        }

        Set<String> userIds = messages.stream()
                .filter(message -> message.getHandledBy() != DiscussionHandledBy.AI)
                .map(DiscussionMessage::getUserId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        User::getId,
                        user -> StringUtils.hasText(user.getFullName()) ? user.getFullName().trim() : user.getUsername(),
                        (left, right) -> left
                ));
    }

    private ProductDiscussionMessageResponse toResponse(DiscussionMessage message, Map<String, String> senderNames) {
        String senderName;
        if (message.getHandledBy() == DiscussionHandledBy.AI) {
            senderName = "AI Assistant";
        } else {
            senderName = senderNames.getOrDefault(message.getUserId(), "Khách hàng");
        }

        return ProductDiscussionMessageResponse.builder()
                .id(message.getId())
                .productId(message.getProductId())
                .userId(message.getUserId())
                .senderName(senderName)
                .content(message.getContent())
                .parentId(message.getParentId())
                .role(message.getRole() == null ? null : message.getRole().name())
                .handledBy(message.getHandledBy() == null ? null : message.getHandledBy().name())
                .aiHandled(Boolean.TRUE.equals(message.getAiHandled()))
                .createdAt(message.getCreatedAt() == null ? Instant.now().toString() : message.getCreatedAt().toInstant().toString())
                .build();
    }
}