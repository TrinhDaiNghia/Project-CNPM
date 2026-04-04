package com.example.demo.services;

import com.example.demo.dtos.response.NotificationResponse;
import com.example.demo.entities.Notification;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.UserRole;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.NotificationRepository;
import com.example.demo.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private static final String REGISTRATION_SUCCESS_TITLE = "\u0110\u0103ng k\u00fd th\u00e0nh c\u00f4ng";
    private static final String PASSWORD_RESET_SUCCESS_TITLE = "\u0110\u1eb7t l\u1ea1i m\u1eadt kh\u1ea9u th\u00e0nh c\u00f4ng";
    private static final String ORDER_SUCCESS_TITLE = "\u0110\u1eb7t h\u00e0ng th\u00e0nh c\u00f4ng";
    private static final String NEW_ORDER_TO_STORE_TITLE = "C\u00f3 \u0111\u01a1n h\u00e0ng m\u1edbi";
    private static final String ORDER_CONFIRMED_TITLE = "\u0110\u01a1n h\u00e0ng \u0111\u00e3 \u0111\u01b0\u1ee3c x\u00e1c nh\u1eadn";
    private static final String ORDER_CANCELLED_BY_STORE_TITLE = "\u0110\u01a1n h\u00e0ng \u0111\u00e3 b\u1ecb h\u1ee7y b\u1edfi c\u1eeda h\u00e0ng";
    private static final String ORDER_DELIVERED_TITLE = "\u0110\u01a1n h\u00e0ng \u0111\u00e3 \u0111\u01b0\u1ee3c giao";
    private static final String ORDER_STATUS_UPDATE_TITLE = "C\u1eadp nh\u1eadt tr\u1ea1ng th\u00e1i \u0111\u01a1n h\u00e0ng";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AccessControlService accessControlService;

    @Value("${notification.system-sender-id:}")
    private String systemSenderId;

    @Value("${notification.system-bot.username:system.bot}")
    private String systemBotUsername;

    @Value("${notification.system-bot.email:system.bot@local}")
    private String systemBotEmail;

    private User requireUser(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("User id is required");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User with id " + userId + " not found"));
    }

    private User getPrivilegedSender(String senderId) {
        User sender = requireUser(senderId);
        if (sender.getRole() != UserRole.STAFF && sender.getRole() != UserRole.OWNER) {
            throw new IllegalStateException("Sender must have STAFF or OWNER role");
        }
        return sender;
    }

    private User getSystemSender() {
        if (StringUtils.hasText(systemSenderId)) {
            return requireUser(systemSenderId);
        }

        User byUsername = StringUtils.hasText(systemBotUsername)
                ? userRepository.findByUsername(systemBotUsername.trim()).orElse(null)
                : null;

        User byEmail = StringUtils.hasText(systemBotEmail)
                ? userRepository.findByEmail(systemBotEmail.trim().toLowerCase()).orElse(null)
                : null;

        if (byUsername != null && byEmail != null && !byUsername.getId().equals(byEmail.getId())) {
            throw new IllegalStateException("System BOT config is ambiguous: username and email point to different users");
        }

        User sender = byUsername != null ? byUsername : byEmail;
        if (sender == null) {
            throw new IllegalStateException("System BOT user not found. Set NOTIFICATION_SYSTEM_SENDER_ID or configure notification.system-bot.*");
        }
        if (sender.getRole() != UserRole.STAFF && sender.getRole() != UserRole.OWNER) {
            throw new IllegalStateException("System sender must have STAFF or OWNER role");
        }
        return sender;
    }

    public void sendRegistrationSuccessNotification(User registeredUser) {
        if (registeredUser == null || !StringUtils.hasText(registeredUser.getId())) {
            throw new IllegalArgumentException("Registered user is required");
        }

        User receiver = requireUser(registeredUser.getId());
        User sender = getSystemSender();

        String content = "Ch\u00e0o " + receiver.getUsername()
                + ", t\u00e0i kho\u1ea3n c\u1ee7a b\u1ea1n \u0111\u00e3 \u0111\u01b0\u1ee3c t\u1ea1o th\u00e0nh c\u00f4ng.";

        Notification notification = Notification.builder()
                .title(REGISTRATION_SUCCESS_TITLE)
                .content(content)
                .directUrl("/profile")
                .sender(sender)
                .receiver(receiver)
                .expiry(Date.from(Instant.now().plus(30, ChronoUnit.DAYS)))
                .build();

        notificationRepository.save(notification);
    }

    public void sendPasswordResetSuccessNotification(String senderId, User user) {
        if (user == null || !StringUtils.hasText(user.getId())) {
            throw new IllegalArgumentException("User is required");
        }
        if (!StringUtils.hasText(senderId)) {
            throw new IllegalStateException("Sender ID is required");
        }

        User sender = getPrivilegedSender(senderId);
        User receiver = requireUser(user.getId());

        String content = "Ch\u00e0o " + receiver.getUsername()
                + ", m\u1eadt kh\u1ea9u c\u1ee7a b\u1ea1n \u0111\u00e3 \u0111\u01b0\u1ee3c \u0111\u1eb7t l\u1ea1i th\u00e0nh c\u00f4ng. Vui l\u00f2ng \u0111\u0103ng nh\u1eadp l\u1ea1i v\u1edbi m\u1eadt kh\u1ea9u m\u1edbi.";

        Notification notification = Notification.builder()
                .title(PASSWORD_RESET_SUCCESS_TITLE)
                .content(content)
                .directUrl("/auth/login")
                .sender(sender)
                .receiver(receiver)
                .expiry(Date.from(Instant.now().plus(30, ChronoUnit.DAYS)))
                .build();

        notificationRepository.save(notification);
    }

    public void sendOrderSuccessNotification(String senderId, User customer, String orderId) {
        if (customer == null || !StringUtils.hasText(customer.getId())) {
            throw new IllegalArgumentException("Customer is required");
        }
        if (!StringUtils.hasText(senderId)) {
            throw new IllegalStateException("Sender ID is required");
        }

        User sender = getPrivilegedSender(senderId);
        User receiver = requireUser(customer.getId());

        String content = "Ch\u00e0o " + receiver.getUsername()
                + ", \u0111\u01a1n h\u00e0ng " + orderId + " \u0111\u00e3 \u0111\u01b0\u1ee3c \u0111\u1eb7t th\u00e0nh c\u00f4ng.";

        Notification notification = Notification.builder()
                .title(ORDER_SUCCESS_TITLE)
                .content(content)
                .directUrl("/orders/" + orderId)
                .sender(sender)
                .receiver(receiver)
                .expiry(Date.from(Instant.now().plus(30, ChronoUnit.DAYS)))
                .build();

        notificationRepository.save(notification);
    }

    public void sendNewOrderToStoreNotification(String senderId, User store, String orderId) {
        if (store == null || !StringUtils.hasText(store.getId())) {
            throw new IllegalArgumentException("Store receiver is required");
        }
        if (!StringUtils.hasText(senderId)) {
            throw new IllegalStateException("Sender ID is required");
        }

        User sender = requireUser(senderId);
        User receiver = requireUser(store.getId());

        String content = "Ch\u00e0o " + receiver.getUsername()
                + ", b\u1ea1n c\u00f3 m\u1ed9t \u0111\u01a1n h\u00e0ng m\u1edbi v\u1edbi m\u00e3 " + orderId + ".";

        Notification notification = Notification.builder()
                .title(NEW_ORDER_TO_STORE_TITLE)
                .content(content)
                .directUrl("/staff/orders/" + orderId)
                .sender(sender)
                .receiver(receiver)
                .expiry(Date.from(Instant.now().plus(30, ChronoUnit.DAYS)))
                .build();

        notificationRepository.save(notification);
    }

    public void sendOrderStatusUpdateNotification(String senderId, User customer, String orderId, String title, String newStatus) {
        if (customer == null || !StringUtils.hasText(customer.getId())) {
            throw new IllegalArgumentException("Customer is required");
        }
        if (!StringUtils.hasText(senderId)) {
            throw new IllegalStateException("Sender ID is required");
        }
        if (!StringUtils.hasText(title)) {
            title = ORDER_STATUS_UPDATE_TITLE;
        }

        User sender = getPrivilegedSender(senderId);
        User receiver = requireUser(customer.getId());

        String content = "Ch\u00e0o " + receiver.getUsername()
                + ", \u0111\u01a1n h\u00e0ng " + orderId + " \u0111\u00e3 \u0111\u01b0\u1ee3c c\u1eadp nh\u1eadt tr\u1ea1ng th\u00e1i: " + newStatus + ".";

        Notification notification = Notification.builder()
                .title(title)
                .content(content)
                .directUrl("/orders/" + orderId)
                .sender(sender)
                .receiver(receiver)
                .expiry(Date.from(Instant.now().plus(30, ChronoUnit.DAYS)))
                .build();

        notificationRepository.save(notification);
    }

    public void sendOrderConfirmedNotification(String senderId, User customer, String orderId) {
        sendOrderStatusUpdateNotification(senderId, customer, orderId, ORDER_CONFIRMED_TITLE, "\u0110\u00e3 \u0111\u01b0\u1ee3c x\u00e1c nh\u1eadn");
    }

    public void sendOrderCancelledByStoreNotification(String senderId, User customer, String orderId) {
        sendOrderStatusUpdateNotification(senderId, customer, orderId, ORDER_CANCELLED_BY_STORE_TITLE, "\u0110\u00e3 b\u1ecb h\u1ee7y b\u1edfi c\u1eeda h\u00e0ng");
    }

    public void sendOrderDeliveredNotification(String senderId, User customer, String orderId) {
        sendOrderStatusUpdateNotification(senderId, customer, orderId, ORDER_DELIVERED_TITLE, "\u0110\u00e3 \u0111\u01b0\u1ee3c giao");
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("userId is required");
        }

        accessControlService.requireUserSelfOrPrivileged(userId);

        return notificationRepository.findByReceiverIdOrderByTimeCreatedDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByUserId(String userId) {
        return getMyNotifications(userId);
    }

    public void markAsRead(String userId, String notificationId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        if (!StringUtils.hasText(notificationId)) {
            throw new IllegalArgumentException("notificationId is required");
        }

        accessControlService.requireUserSelfOrPrivileged(userId);

        Notification notification = notificationRepository.findByIdAndReceiverId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    public int markAllAsRead(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("userId is required");
        }

        accessControlService.requireUserSelfOrPrivileged(userId);
        return notificationRepository.markAllAsReadByReceiverId(userId);
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .directUrl(notification.getDirectUrl())
                .isRead(notification.isRead())
                .timeCreated(notification.getTimeCreated())
                .expiry(notification.getExpiry())
                .senderId(notification.getSender() != null ? notification.getSender().getId() : null)
                .receiverId(notification.getReceiver() != null ? notification.getReceiver().getId() : null)
                .build();
    }
}
