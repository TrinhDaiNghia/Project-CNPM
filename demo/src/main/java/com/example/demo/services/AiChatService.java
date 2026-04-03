package com.example.demo.services;

import com.example.demo.entities.Customer;
import com.example.demo.entities.Discuss;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.UserRole;
import com.example.demo.repositories.CustomerRepository;
import com.example.demo.repositories.DiscussRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatService {

    private static final String BOT_DISPLAY_NAME = "System Bot";

    private final QdrantService qdrantService;
    private final GeminiService geminiService;
    private final AccessControlService accessControlService;
    private final CustomerRepository customerRepository;
    private final DiscussRepository discussRepository;

    @Transactional
    public String chatWithBot(String userMessage) {
        log.info("Customer message: {}", userMessage);

        String context = qdrantService.searchRelevantContext(userMessage);
        String botResponse = geminiService.generateAnswer(context, userMessage);

        saveDiscussionIfCustomer(userMessage, botResponse);

        log.info("{} response: {}", BOT_DISPLAY_NAME, botResponse);
        return botResponse;
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
            // Keep chat flow alive even if persistence fails.
            log.warn("Failed to persist AI discussion history", ex);
        }
    }
}

