package com.example.demo.services;

import com.example.demo.entities.Chat;
import com.example.demo.entities.Customer;
import com.example.demo.entities.ChatMessage;
import com.example.demo.entities.Product;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.ChatHandledBy;
import com.example.demo.entities.enums.ChatMessageRole;
import com.example.demo.entities.enums.ProductStatus;
import com.example.demo.entities.enums.UserRole;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.ChatRepository;
import com.example.demo.repositories.CustomerRepository;
import com.example.demo.repositories.ChatMessageRepository;
import com.example.demo.repositories.ProductRepository;
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

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

    private static final String BOT_DISPLAY_NAME = "System Bot";
    private static final String STAFF_DISPLAY_NAME = "Staff";
    private static final String STAFF_HANDOFF_MESSAGE = "Yêu cầu của bạn đã được chuyển cho nhân viên hỗ trợ. Vui lòng chờ phản hồi.";
    private static final String ESCALATE_CONFIRM_MESSAGE = "Đã chuyển cuộc trò chuyện cho nhân viên hỗ trợ. Vui lòng để lại thêm thông tin để được phản hồi sớm.";

    private final QdrantService qdrantService;
    private final GeminiService geminiService;
    private final AccessControlService accessControlService;
    private final CustomerRepository customerRepository;
    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ProductRepository productRepository;

    @Transactional
    public AiChatResult chatWithBot(String userMessage) {
        log.info("Customer message: {}", userMessage);

        Optional<Chat> activeStaffSession = findActiveStaffSessionForCurrentCustomer();
        if (activeStaffSession.isPresent()) {
            appendCustomerMessage(activeStaffSession.get(), userMessage, ChatHandledBy.STAFF);
            return new AiChatResult(STAFF_HANDOFF_MESSAGE, "STAFF");
        }

        String context = buildChatContext(userMessage);
        List<Message> history = buildRecentAiHistoryForCurrentCustomer();
        String botResponse = geminiService.generateAnswer(context, userMessage, history);
        saveAiConversationIfCustomer(userMessage, botResponse);

        log.info("{} response: {}", BOT_DISPLAY_NAME, botResponse);
        return new AiChatResult(botResponse, "AI");
    }

    @Transactional
    public AiChatResult escalateToStaff() {
        User currentUser = accessControlService.getCurrentUserOrThrow();
        if (currentUser.getRole() != UserRole.CUSTOMER) {
            throw new IllegalStateException("Only CUSTOMER can request support handoff");
        }

        Customer customer = ensureCustomerProfile(currentUser);
        Chat chat = findOpenSupportSessionWithCustomer(customer.getId())
                .orElseGet(() -> createSupportChat(customer));

        saveMessage(
                chat,
                ChatMessageRole.ASSISTANT,
                ChatHandledBy.STAFF,
                BOT_DISPLAY_NAME,
                ESCALATE_CONFIRM_MESSAGE
        );

        return new AiChatResult(ESCALATE_CONFIRM_MESSAGE, "STAFF");
    }

    @Transactional(readOnly = true)
    public List<Chat> getOpenStaffSupportChats() {
        accessControlService.requirePrivilegedRole();
        return chatRepository.findOpenSupportChatsWithCustomer();
    }

    @Transactional(readOnly = true)
    public List<Chat> getAllStaffSupportChats() {
        accessControlService.requirePrivilegedRole();
        return chatRepository.findAllSupportChatsWithCustomer();
    }

    @Transactional(readOnly = true)
    public Page<Chat> getStaffSupportChatsPage(String status, Pageable pageable) {
        accessControlService.requirePrivilegedRole();

        String normalized = status == null ? "ALL" : status.trim().toUpperCase();
        return switch (normalized) {
            case "OPEN" -> chatRepository.findOpenSupportChatsPageWithCustomer(pageable);
            case "CLOSED" -> chatRepository.findClosedSupportChatsPageWithCustomer(pageable);
            default -> chatRepository.findAllSupportChatsPageWithCustomer(pageable);
        };
    }

    @Transactional(readOnly = true)
    public Optional<Chat> getMyOpenSupportChat() {
        User currentUser = accessControlService.getCurrentUserOrThrow();
        if (currentUser.getRole() != UserRole.CUSTOMER) {
            return Optional.empty();
        }
        return findOpenSupportSessionWithCustomer(currentUser.getId());
    }

    @Transactional(readOnly = true)
    public List<ChatHistoryMessage> getMyChatHistory() {
        User currentUser = accessControlService.getCurrentUserOrThrow();
        if (currentUser.getRole() != UserRole.CUSTOMER) {
            return List.of();
        }

        return chatMessageRepository.findByChatCustomerIdOrderByCreatedAtAsc(currentUser.getId()).stream()
                .filter(row -> StringUtils.hasText(row.getContent()))
                .map(row -> new ChatHistoryMessage(
                        row.getId(),
                        toRoleValue(row.getRole()),
                        row.getContent(),
                        toCreatedAtValue(row.getCreatedAt()),
                        row.getHandledBy() == null ? "AI" : row.getHandledBy().name()
                ))
                .toList();
    }

    @Transactional
    public void replySupportChat(String chatId, String message) {
        accessControlService.requirePrivilegedRole();
        if (!StringUtils.hasText(message)) {
            throw new IllegalArgumentException("Message is required");
        }

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found: " + chatId));
        if (chat.getEndDate() != null) {
            throw new IllegalStateException("This support chat is already closed");
        }

        User staff = accessControlService.getCurrentUserOrThrow();
        String staffName = StringUtils.hasText(staff.getFullName()) ? staff.getFullName() : staff.getUsername();
        saveMessage(
                chat,
                ChatMessageRole.ASSISTANT,
                ChatHandledBy.STAFF,
                staffName,
                message.trim()
        );
    }

    @Transactional
    public void closeSupportChat(String chatId) {
        accessControlService.requirePrivilegedRole();

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found: " + chatId));
        if (chat.getEndDate() != null) {
            return;
        }

        User staff = accessControlService.getCurrentUserOrThrow();
        String staffName = StringUtils.hasText(staff.getFullName()) ? staff.getFullName() : staff.getUsername();

        saveMessage(
                chat,
                ChatMessageRole.SYSTEM,
                ChatHandledBy.STAFF,
                "System",
                "Support closed by staff (" + staffName + ")"
        );

        chat.setEndDate(new Date());
        chatRepository.save(chat);
    }

    @Transactional(readOnly = true)
    public String buildChatContentLog(Chat chat) {
        if (chat == null || !StringUtils.hasText(chat.getId())) {
            return "";
        }

        List<ChatMessage> messages = chatMessageRepository.findByChatIdOrderByCreatedAtAsc(chat.getId());
        return messages.stream()
                .filter(message -> StringUtils.hasText(message.getContent()))
                .map(this::formatSupportLogLine)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private List<Message> buildRecentAiHistoryForCurrentCustomer() {
        try {
            User currentUser = accessControlService.getCurrentUserOrThrow();
            if (currentUser.getRole() != UserRole.CUSTOMER) {
                return List.of();
            }

            List<ChatMessage> latestRows =
                    chatMessageRepository.findByChatCustomerIdAndChatIsAiHandledTrueOrderByCreatedAtDesc(
                            currentUser.getId(),
                            PageRequest.of(0, 10)
                    );
            if (latestRows.isEmpty()) {
                return List.of();
            }

            Collections.reverse(latestRows);

            List<Message> history = new ArrayList<>();
            for (ChatMessage row : latestRows) {
                if (!StringUtils.hasText(row.getContent())) {
                    continue;
                }
                if (row.getRole() == ChatMessageRole.USER) {
                    history.add(new UserMessage(row.getContent()));
                } else if (row.getRole() == ChatMessageRole.ASSISTANT) {
                    history.add(new AssistantMessage(row.getContent()));
                }
            }
            return history;
        } catch (Exception ex) {
            log.warn("Failed to build AI chat history", ex);
            return List.of();
        }
    }

    private void saveAiConversationIfCustomer(String userMessage, String botResponse) {
        User currentUser = accessControlService.getCurrentUserOrThrow();
        if (currentUser.getRole() != UserRole.CUSTOMER) {
            return;
        }

        Customer customer = ensureCustomerProfile(currentUser);

        Date now = new Date();
        Chat chat = Chat.builder()
                .startDate(now)
                .endDate(now)
                .isAiHandled(true)
                .customer(customer)
                .build();
        Chat saved = chatRepository.saveAndFlush(chat);

        saveMessage(saved, ChatMessageRole.USER, ChatHandledBy.AI, "Customer", userMessage);
        saveMessage(saved, ChatMessageRole.ASSISTANT, ChatHandledBy.AI, BOT_DISPLAY_NAME, botResponse);
    }

    private Optional<Chat> findActiveStaffSessionForCurrentCustomer() {
        try {
            User currentUser = accessControlService.getCurrentUserOrThrow();
            if (currentUser.getRole() != UserRole.CUSTOMER) {
                return Optional.empty();
            }
            return findOpenSupportSessionWithCustomer(currentUser.getId());
        } catch (Exception ex) {
            log.warn("Failed to check active staff support session", ex);
            return Optional.empty();
        }
    }

    private Optional<Chat> findOpenSupportSessionWithCustomer(String customerId) {
        return chatRepository.findFirstByCustomerIdAndIsAiHandledFalseAndEndDateIsNullOrderByStartDateDesc(customerId);
    }

    private Chat createSupportChat(Customer customer) {
        Chat chat = Chat.builder()
                .startDate(new Date())
                .isAiHandled(false)
                .customer(customer)
                .build();
        return chatRepository.saveAndFlush(chat);
    }

    private void appendCustomerMessage(Chat chat, String message, ChatHandledBy handledBy) {
        saveMessage(chat, ChatMessageRole.USER, handledBy, "Customer", message);
    }

    private void saveMessage(Chat chat,
                             ChatMessageRole role,
                             ChatHandledBy handledBy,
                             String senderName,
                             String content) {
        if (chat == null || !StringUtils.hasText(content)) {
            return;
        }

        ChatMessage row = ChatMessage.builder()
                .chat(chat)
                .role(role)
                .handledBy(handledBy)
                .senderName(senderName)
                .content(content.trim())
                .build();
        chatMessageRepository.saveAndFlush(row);
    }

    private String formatSupportLogLine(ChatMessage message) {
        String content = message.getContent().trim();
        if (message.getRole() == ChatMessageRole.USER) {
            return "Customer: " + content;
        }
        if (message.getRole() == ChatMessageRole.ASSISTANT) {
            if (message.getHandledBy() == ChatHandledBy.STAFF) {
                String sender = StringUtils.hasText(message.getSenderName()) ? message.getSenderName() : STAFF_DISPLAY_NAME;
                return "Staff (" + sender + "): " + content;
            }
            return BOT_DISPLAY_NAME + ": " + content;
        }
        return "System: " + content;
    }

    private Customer ensureCustomerProfile(User currentUser) {
        return customerRepository.findById(currentUser.getId())
                .orElseGet(() -> {
                    log.warn("Missing customer profile for CUSTOMER user {}, creating fallback profile", currentUser.getId());
                    Customer customer = new Customer();
                    customer.setId(currentUser.getId());
                    return customerRepository.saveAndFlush(customer);
                });
    }

    private String toRoleValue(ChatMessageRole role) {
        if (role == null) {
            return "assistant";
        }
        return switch (role) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
        };
    }

    private String toCreatedAtValue(Date createdAt) {
        if (createdAt == null) {
            return Instant.now().toString();
        }
        return createdAt.toInstant().toString();
    }

    private String buildChatContext(String userMessage) {
        String qdrantContext = qdrantService.searchRelevantContext(userMessage);
        String inventoryContext = shouldIncludeInventoryContext(userMessage) ? buildInventoryContext() : "";

        if (!StringUtils.hasText(inventoryContext)) {
            return qdrantContext;
        }
        if (!StringUtils.hasText(qdrantContext)) {
            return inventoryContext;
        }
        return qdrantContext + "\n---\n" + inventoryContext;
    }

    private boolean shouldIncludeInventoryContext(String userMessage) {
        if (!StringUtils.hasText(userMessage)) {
            return false;
        }

        String normalized = normalizeForIntent(userMessage);
        return normalized.contains("ban gi")
                || normalized.contains("san pham")
                || normalized.contains("so luong")
                || normalized.contains("ton kho")
                || normalized.contains("con hang")
                || normalized.contains("liet ke")
                || normalized.contains("danh muc")
                || normalized.contains("bao nhieu");
    }

    private String buildInventoryContext() {
        List<Product> activeProducts = productRepository.findByStatus(ProductStatus.ACTIVE).stream()
                .sorted(Comparator.comparing(product -> safeText(product.getName()), String.CASE_INSENSITIVE_ORDER))
                .toList();

        if (activeProducts.isEmpty()) {
            return "DU LIEU TON KHO TOAN CUA HANG: khong co san pham dang ban.";
        }

        int totalStock = activeProducts.stream()
                .map(Product::getStockQuantity)
                .filter(value -> value != null && value > 0)
                .mapToInt(Integer::intValue)
                .sum();

        int maxItems = 40;
        StringBuilder builder = new StringBuilder();
        builder.append("DU LIEU TON KHO TOAN CUA HANG: ")
                .append("tong so mau dang ban = ").append(activeProducts.size())
                .append(", tong so luong ton = ").append(totalStock).append(".")
                .append("\n");

        int limit = Math.min(maxItems, activeProducts.size());
        for (int i = 0; i < limit; i++) {
            Product product = activeProducts.get(i);
            int stock = product.getStockQuantity() == null ? 0 : Math.max(0, product.getStockQuantity());
            String brand = StringUtils.hasText(product.getBrand()) ? product.getBrand().trim() : "Khong ro thuong hieu";
            builder.append("San pham ")
                    .append(i + 1)
                    .append(": id=").append(product.getId())
                    .append(", ten=").append(product.getName())
                    .append(", thuong hieu=").append(brand)
                    .append(", so luong ton=").append(stock)
                    .append(".\n");
        }
        if (activeProducts.size() > limit) {
            builder.append("Con ")
                    .append(activeProducts.size() - limit)
                    .append(" san pham dang ban khac khong hien thi trong danh sach rut gon.");
        }
        return builder.toString().trim();
    }

    private String normalizeForIntent(String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        String noAccent = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return noAccent.replace('đ', 'd')
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    public record AiChatResult(String message, String handledBy) {
    }

    public record ChatHistoryMessage(String id, String role, String content, String createdAt, String handledBy) {
    }
}


