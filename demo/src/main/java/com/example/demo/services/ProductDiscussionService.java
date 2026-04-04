package com.example.demo.services;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductDiscussionService {

    private final DiscussionMessageRepository discussionMessageRepository;
    private final ProductRepository productRepository;
    private final QdrantService qdrantService;
    private final GeminiService geminiService;
    private final AccessControlService accessControlService;

    @Transactional(readOnly = true)
    public List<ProductDiscussionMessageResponse> listByProduct(String productId) {
        ensureProductExists(productId);
        return discussionMessageRepository.findByProductIdOrderByCreatedAtAsc(productId).stream()
                .map(this::toResponse)
                .toList();
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

        String context = qdrantService.searchRelevantContextByProduct(productId, normalizedQuestion);
        if (!StringUtils.hasText(context)) {
            context = buildFallbackContext(product);
        }

        String answer = geminiService.generateAnswer(context, normalizedQuestion, history);
        if (!StringUtils.hasText(answer)) {
            answer = "Xin loi, hien tai he thong chua the tra loi cau hoi nay. Vui long thu lai sau.";
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

        return ProductDiscussionAskResponse.builder()
                .question(toResponse(questionMessage))
                .answer(toResponse(answerMessage))
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

    private String buildFallbackContext(Product product) {
        return String.format(
                "Product: %s. Brand: %s. Price: %s VND. Stock quantity: %d. Movement type: %s. Glass material: %s. " +
                        "Water resistance: %s. Face size: %s. Strap material: %s. Strap color: %s. Case color: %s. " +
                        "Face color: %s. Description: %s.",
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
        return StringUtils.hasText(value) ? value.trim() : "unknown";
    }

    private String formatPrice(Long price) {
        if (price == null) {
            return "unknown";
        }
        return String.format("%,d", price);
    }

    private ProductDiscussionMessageResponse toResponse(DiscussionMessage message) {
        return ProductDiscussionMessageResponse.builder()
                .id(message.getId())
                .productId(message.getProductId())
                .userId(message.getUserId())
                .content(message.getContent())
                .parentId(message.getParentId())
                .role(message.getRole() == null ? null : message.getRole().name())
                .handledBy(message.getHandledBy() == null ? null : message.getHandledBy().name())
                .aiHandled(Boolean.TRUE.equals(message.getAiHandled()))
                .createdAt(message.getCreatedAt() == null ? Instant.now().toString() : message.getCreatedAt().toInstant().toString())
                .build();
    }
}
