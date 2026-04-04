package com.example.demo.services;

import com.example.demo.entities.Customer;
import com.example.demo.entities.Discuss;
import com.example.demo.entities.DiscussionMessage;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.DiscussionHandledBy;
import com.example.demo.entities.enums.DiscussionMessageRole;
import com.example.demo.entities.enums.UserRole;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.CustomerRepository;
import com.example.demo.repositories.DiscussRepository;
import com.example.demo.repositories.DiscussionMessageRepository;
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
import java.util.Date;
import java.util.List;
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
    private final DiscussRepository discussRepository;
    private final DiscussionMessageRepository discussionMessageRepository;

    @Transactional
    public AiChatResult chatWithBot(String userMessage) {
        log.info("Customer message: {}", userMessage);

        Optional<Discuss> activeStaffSession = findActiveStaffSessionForCurrentCustomer();
        if (activeStaffSession.isPresent()) {
            appendCustomerMessage(activeStaffSession.get(), userMessage, DiscussionHandledBy.STAFF);
            return new AiChatResult(STAFF_HANDOFF_MESSAGE, "STAFF");
        }

        String context = qdrantService.searchRelevantContext(userMessage);
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
        Discuss discuss = findOpenSupportSessionWithCustomer(customer.getId())
                .orElseGet(() -> createSupportDiscussion(customer));

        saveMessage(
                discuss,
                DiscussionMessageRole.ASSISTANT,
                DiscussionHandledBy.STAFF,
                BOT_DISPLAY_NAME,
                ESCALATE_CONFIRM_MESSAGE
        );

        return new AiChatResult(ESCALATE_CONFIRM_MESSAGE, "STAFF");
    }

    @Transactional(readOnly = true)
    public List<Discuss> getOpenStaffSupportDiscussions() {
        accessControlService.requirePrivilegedRole();
        return discussRepository.findOpenSupportDiscussionsWithCustomer();
    }

    @Transactional(readOnly = true)
    public List<Discuss> getAllStaffSupportDiscussions() {
        accessControlService.requirePrivilegedRole();
        return discussRepository.findAllSupportDiscussionsWithCustomer();
    }

    @Transactional(readOnly = true)
    public Page<Discuss> getStaffSupportDiscussionsPage(String status, Pageable pageable) {
        accessControlService.requirePrivilegedRole();

        String normalized = status == null ? "ALL" : status.trim().toUpperCase();
        return switch (normalized) {
            case "OPEN" -> discussRepository.findOpenSupportDiscussionsPageWithCustomer(pageable);
            case "CLOSED" -> discussRepository.findClosedSupportDiscussionsPageWithCustomer(pageable);
            default -> discussRepository.findAllSupportDiscussionsPageWithCustomer(pageable);
        };
    }

    @Transactional(readOnly = true)
    public Optional<Discuss> getMyOpenSupportDiscussion() {
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

        return discussionMessageRepository.findByDiscussCustomerIdOrderByCreatedAtAsc(currentUser.getId()).stream()
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
    public void replySupportDiscussion(String discussionId, String message) {
        accessControlService.requirePrivilegedRole();
        if (!StringUtils.hasText(message)) {
            throw new IllegalArgumentException("Message is required");
        }

        Discuss discuss = discussRepository.findById(discussionId)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion not found: " + discussionId));
        if (discuss.getEndDate() != null) {
            throw new IllegalStateException("This support discussion is already closed");
        }

        User staff = accessControlService.getCurrentUserOrThrow();
        String staffName = StringUtils.hasText(staff.getFullName()) ? staff.getFullName() : staff.getUsername();
        saveMessage(
                discuss,
                DiscussionMessageRole.ASSISTANT,
                DiscussionHandledBy.STAFF,
                staffName,
                message.trim()
        );
    }

    @Transactional
    public void closeSupportDiscussion(String discussionId) {
        accessControlService.requirePrivilegedRole();

        Discuss discuss = discussRepository.findById(discussionId)
                .orElseThrow(() -> new ResourceNotFoundException("Discussion not found: " + discussionId));
        if (discuss.getEndDate() != null) {
            return;
        }

        User staff = accessControlService.getCurrentUserOrThrow();
        String staffName = StringUtils.hasText(staff.getFullName()) ? staff.getFullName() : staff.getUsername();

        saveMessage(
                discuss,
                DiscussionMessageRole.SYSTEM,
                DiscussionHandledBy.STAFF,
                "System",
                "Support closed by staff (" + staffName + ")"
        );

        discuss.setEndDate(new Date());
        discussRepository.save(discuss);
    }

    @Transactional(readOnly = true)
    public String buildDiscussionContentLog(Discuss discuss) {
        if (discuss == null || !StringUtils.hasText(discuss.getId())) {
            return "";
        }

        List<DiscussionMessage> messages = discussionMessageRepository.findByDiscussIdOrderByCreatedAtAsc(discuss.getId());
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

            List<DiscussionMessage> latestRows =
                    discussionMessageRepository.findByDiscussCustomerIdAndDiscussIsAiHandledTrueOrderByCreatedAtDesc(
                            currentUser.getId(),
                            PageRequest.of(0, 10)
                    );
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
        Discuss discuss = Discuss.builder()
                .startDate(now)
                .endDate(now)
                .isAiHandled(true)
                .customer(customer)
                .build();
        Discuss saved = discussRepository.saveAndFlush(discuss);

        saveMessage(saved, DiscussionMessageRole.USER, DiscussionHandledBy.AI, "Customer", userMessage);
        saveMessage(saved, DiscussionMessageRole.ASSISTANT, DiscussionHandledBy.AI, BOT_DISPLAY_NAME, botResponse);
    }

    private Optional<Discuss> findActiveStaffSessionForCurrentCustomer() {
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

    private Optional<Discuss> findOpenSupportSessionWithCustomer(String customerId) {
        return discussRepository.findFirstByCustomerIdAndIsAiHandledFalseAndEndDateIsNullOrderByStartDateDesc(customerId);
    }

    private Discuss createSupportDiscussion(Customer customer) {
        Discuss discuss = Discuss.builder()
                .startDate(new Date())
                .isAiHandled(false)
                .customer(customer)
                .build();
        return discussRepository.saveAndFlush(discuss);
    }

    private void appendCustomerMessage(Discuss discuss, String message, DiscussionHandledBy handledBy) {
        saveMessage(discuss, DiscussionMessageRole.USER, handledBy, "Customer", message);
    }

    private void saveMessage(Discuss discuss,
                             DiscussionMessageRole role,
                             DiscussionHandledBy handledBy,
                             String senderName,
                             String content) {
        if (discuss == null || !StringUtils.hasText(content)) {
            return;
        }

        DiscussionMessage row = DiscussionMessage.builder()
                .discuss(discuss)
                .role(role)
                .handledBy(handledBy)
                .senderName(senderName)
                .content(content.trim())
                .build();
        discussionMessageRepository.saveAndFlush(row);
    }

    private String formatSupportLogLine(DiscussionMessage message) {
        String content = message.getContent().trim();
        if (message.getRole() == DiscussionMessageRole.USER) {
            return "Customer: " + content;
        }
        if (message.getRole() == DiscussionMessageRole.ASSISTANT) {
            if (message.getHandledBy() == DiscussionHandledBy.STAFF) {
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

    private String toRoleValue(DiscussionMessageRole role) {
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

    public record AiChatResult(String message, String handledBy) {
    }

    public record ChatHistoryMessage(String id, String role, String content, String createdAt, String handledBy) {
    }
}
