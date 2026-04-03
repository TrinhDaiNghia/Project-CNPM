package com.example.demo.services;

import com.example.demo.entities.Customer;
import com.example.demo.entities.Discuss;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.UserRole;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.CustomerRepository;
import com.example.demo.repositories.DiscussRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

    private static final String BOT_DISPLAY_NAME = "System Bot";
    private static final String STAFF_HANDOFF_MESSAGE = "Yêu cầu của bạn đã được chuyển cho nhân viên hỗ trợ. Vui lòng chờ phản hồi.";

    private final QdrantService qdrantService;
    private final GeminiService geminiService;
    private final AccessControlService accessControlService;
    private final CustomerRepository customerRepository;
    private final DiscussRepository discussRepository;

    @Transactional
    public AiChatResult chatWithBot(String userMessage) {
        log.info("Customer message: {}", userMessage);

        Optional<Discuss> activeStaffSession = findActiveStaffSessionForCurrentCustomer();
        if (activeStaffSession.isPresent()) {
            appendCustomerMessage(activeStaffSession.get(), userMessage);
            return new AiChatResult(STAFF_HANDOFF_MESSAGE, "STAFF");
        }

        String context = qdrantService.searchRelevantContext(userMessage);
        String botResponse = geminiService.generateAnswer(context, userMessage);
        saveDiscussionIfCustomer(userMessage, botResponse);

        log.info("{} response: {}", BOT_DISPLAY_NAME, botResponse);
        return new AiChatResult(botResponse, "AI");
    }

    @Transactional
    public AiChatResult escalateToStaff() {
        User currentUser = accessControlService.getCurrentUserOrThrow();
        if (currentUser.getRole() != UserRole.CUSTOMER) {
            throw new IllegalStateException("Only CUSTOMER can request support handoff");
        }

        Customer customer = customerRepository.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalStateException("Customer profile not found for current account"));

        Optional<Discuss> activeSession = findOpenSupportSessionWithCustomer(customer.getId());
        if (activeSession.isEmpty()) {
            Discuss discuss = Discuss.builder()
                    .startDate(new Date())
                    .contentLog("ESCALATION_REQUEST: Customer requested handoff to support staff.")
                    .isAiHandled(false)
                    .customer(customer)
                    .build();
            discussRepository.save(discuss);
        }

        return new AiChatResult(
                "Đã chuyển cuộc trò chuyện cho nhân viên hỗ trợ. Vui lòng để lại thêm thông tin để được phản hồi sớm.",
                "STAFF"
        );
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

        List<Discuss> discussions = discussRepository.findByCustomerIdOrderByStartDateAsc(currentUser.getId());
        List<ChatHistoryMessage> messages = new ArrayList<>();
        int seq = 0;

        for (Discuss discuss : discussions) {
            if (!StringUtils.hasText(discuss.getContentLog())) {
                continue;
            }

            Instant base = discuss.getStartDate() != null ? discuss.getStartDate().toInstant() : Instant.now();
            String[] lines = discuss.getContentLog().split("\\r?\\n");
            for (String rawLine : lines) {
                String line = rawLine == null ? "" : rawLine.trim();
                if (!StringUtils.hasText(line)) {
                    continue;
                }

                ParsedMessage parsed = parseMessageLine(line, discuss.getIsAiHandled() != null && discuss.getIsAiHandled());
                if (parsed == null) {
                    continue;
                }

                String messageId = (StringUtils.hasText(discuss.getId()) ? discuss.getId() : "chat") + "-" + seq;
                String createdAt = base.plusMillis(seq).toString();
                messages.add(new ChatHistoryMessage(messageId, parsed.role(), parsed.content(), createdAt, parsed.handledBy()));
                seq++;
            }
        }

        return messages;
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
        appendLine(discuss, "Staff (" + staffName + "): " + message.trim());
        discussRepository.save(discuss);
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
        appendLine(discuss, "System: Support closed by staff (" + staffName + ")");
        discuss.setEndDate(new Date());
        discussRepository.save(discuss);
    }

    private void saveDiscussionIfCustomer(String userMessage, String botResponse) {
        try {
            User currentUser = accessControlService.getCurrentUserOrThrow();
            if (currentUser.getRole() != UserRole.CUSTOMER) {
                return;
            }

            Customer customer = customerRepository.findById(currentUser.getId()).orElse(null);
            if (customer == null) {
                log.warn("Authenticated CUSTOMER user {} not found in customers table", currentUser.getId());
                return;
            }

            Date now = new Date();
            String contentLog = "Customer: " + userMessage + "\n" + BOT_DISPLAY_NAME + ": " + botResponse;

            Discuss discuss = Discuss.builder()
                    .startDate(now)
                    .endDate(now)
                    .contentLog(contentLog)
                    .isAiHandled(true)
                    .customer(customer)
                    .build();

            discussRepository.save(discuss);
        } catch (Exception ex) {
            log.warn("Failed to persist AI discussion history", ex);
        }
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
        List<Discuss> rows = discussRepository.findOpenSupportByCustomerIdWithCustomer(customerId);
        if (rows == null || rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    private void appendCustomerMessage(Discuss discuss, String message) {
        appendLine(discuss, "Customer: " + message);
        discussRepository.save(discuss);
    }

    private void appendLine(Discuss discuss, String line) {
        String existing = discuss.getContentLog();
        if (!StringUtils.hasText(existing)) {
            discuss.setContentLog(line);
            return;
        }
        discuss.setContentLog(existing + "\n" + line);
    }

    private ParsedMessage parseMessageLine(String line, boolean aiHandledDiscussion) {
        if (line.startsWith("Customer:")) {
            return new ParsedMessage("user", line.substring("Customer:".length()).trim(), aiHandledDiscussion ? "AI" : "STAFF");
        }
        if (line.startsWith("System Bot:")) {
            return new ParsedMessage("assistant", line.substring("System Bot:".length()).trim(), "AI");
        }
        if (line.startsWith("Staff (")) {
            int idx = line.indexOf(":");
            String content = idx >= 0 ? line.substring(idx + 1).trim() : line;
            return new ParsedMessage("assistant", content, "STAFF");
        }
        if (line.startsWith("ESCALATION_REQUEST:")) {
            return new ParsedMessage("assistant", "Đã chuyển cuộc trò chuyện cho nhân viên hỗ trợ.", "STAFF");
        }
        if (line.startsWith("System:")) {
            return new ParsedMessage("assistant", line.substring("System:".length()).trim(), "STAFF");
        }
        return new ParsedMessage("assistant", line, aiHandledDiscussion ? "AI" : "STAFF");
    }

    public record AiChatResult(String message, String handledBy) {
    }

    public record ChatHistoryMessage(String id, String role, String content, String createdAt, String handledBy) {
    }

    private record ParsedMessage(String role, String content, String handledBy) {
    }
}
