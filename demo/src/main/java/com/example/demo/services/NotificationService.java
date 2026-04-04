package com.example.demo.services;

import com.example.demo.dtos.response.NotificationResponse;
import com.example.demo.entities.Notification;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.NotificationType;
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

    private static final String REGISTRATION_SUCCESS_TITLE = "Dang ky thanh cong";
    private static final String PASSWORD_RESET_SUCCESS_TITLE = "Dat lai mat khau thanh cong";
    private static final String ORDER_SUCCESS_TITLE = "Dat hang thanh cong";
    private static final String NEW_ORDER_TO_STORE_TITLE = "Co don hang moi";
    private static final String ORDER_CONFIRMED_TITLE = "Don hang da duoc xac nhan";
    private static final String ORDER_CANCELLED_BY_STORE_TITLE = "Don hang da bi huy boi cua hang";
    private static final String ORDER_DELIVERED_TITLE = "Don hang da duoc giao";
    private static final String ORDER_STATUS_UPDATE_TITLE = "Cap nhat trang thai don hang";

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

        String content = "Chao " + receiver.getUsername() + ", tai khoan cua ban da duoc tao thanh cong.";

        Notification notification = Notification.builder()
                .title(REGISTRATION_SUCCESS_TITLE)
                .content(content)
                .type(NotificationType.SYSTEM)
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

        String content = "Chao " + receiver.getUsername() + ", mat khau cua ban da duoc dat lai thanh cong.";

        Notification notification = Notification.builder()
                .title(PASSWORD_RESET_SUCCESS_TITLE)
                .content(content)
                .type(NotificationType.SYSTEM)
                .directUrl("/auth/login")
                .sender(sender)
                .receiver(receiver)
                .expiry(Date.from(Instant.now().plus(30, ChronoUnit.DAYS)))
                .build();

        notificationRepository.save(notification);
    }

    public void sendPasswordResetSuccessNotification(User user) {
        sendPasswordResetSuccessNotification(getSystemSender().getId(), user);
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

        String content = "Chao " + receiver.getUsername() + ", don hang " + orderId + " da duoc dat thanh cong.";

        Notification notification = Notification.builder()
                .title(ORDER_SUCCESS_TITLE)
                .content(content)
                .type(NotificationType.ORDER)
                .directUrl("/orders/" + orderId)
                .sender(sender)
                .receiver(receiver)
                .expiry(Date.from(Instant.now().plus(30, ChronoUnit.DAYS)))
                .build();

        notificationRepository.save(notification);
    }

    public void sendOrderSuccessNotification(User customer, String orderId) {
        sendOrderSuccessNotification(getSystemSender().getId(), customer, orderId);
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

        String content = "Chao " + receiver.getUsername() + ", ban co mot don hang moi voi ma " + orderId + ".";

        Notification notification = Notification.builder()
                .title(NEW_ORDER_TO_STORE_TITLE)
                .content(content)
                .type(NotificationType.ORDER)
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

        String content = "Chao " + receiver.getUsername() + ", don hang " + orderId + " da duoc cap nhat trang thai: " + newStatus + ".";

        Notification notification = Notification.builder()
                .title(title)
                .content(content)
                .type(NotificationType.ORDER)
                .directUrl("/orders/" + orderId)
                .sender(sender)
                .receiver(receiver)
                .expiry(Date.from(Instant.now().plus(30, ChronoUnit.DAYS)))
                .build();

        notificationRepository.save(notification);
    }

    public void sendOrderConfirmedNotification(String senderId, User customer, String orderId) {
        sendOrderStatusUpdateNotification(senderId, customer, orderId, ORDER_CONFIRMED_TITLE, "DA_XAC_NHAN");
    }

    public void sendOrderCancelledByStoreNotification(String senderId, User customer, String orderId) {
        sendOrderStatusUpdateNotification(senderId, customer, orderId, ORDER_CANCELLED_BY_STORE_TITLE, "DA_HUY");
    }

    public void sendOrderDeliveredNotification(String senderId, User customer, String orderId) {
        sendOrderStatusUpdateNotification(senderId, customer, orderId, ORDER_DELIVERED_TITLE, "DA_GIAO");
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(String userId) {
        return getMyNotifications(userId, null, null);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(String userId, Boolean isRead, NotificationType type) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("userId is required");
        }

        accessControlService.requireUserSelfOrPrivileged(userId);

        List<Notification> notifications;
        if (isRead != null && type != null) {
            notifications = notificationRepository.findByReceiverIdAndReadAndTypeOrderByTimeCreatedDesc(userId, isRead, type);
        } else if (isRead != null) {
            notifications = notificationRepository.findByReceiverIdAndReadOrderByTimeCreatedDesc(userId, isRead);
        } else if (type != null) {
            notifications = notificationRepository.findByReceiverIdAndTypeOrderByTimeCreatedDesc(userId, type);
        } else {
            notifications = notificationRepository.findByReceiverIdOrderByTimeCreatedDesc(userId);
        }

        return notifications.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByUserId(String userId) {
        return getMyNotifications(userId, null, null);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByUserId(String userId, Boolean isRead, NotificationType type) {
        return getMyNotifications(userId, isRead, type);
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
            notification.setReadAt(Date.from(Instant.now()));
            notificationRepository.save(notification);
        }
    }

    public int markAllAsRead(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("userId is required");
        }

        accessControlService.requireUserSelfOrPrivileged(userId);
        return notificationRepository.markAllAsReadByReceiverId(userId, Date.from(Instant.now()));
    }

    public void deleteNotification(String userId, String notificationId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("userId is required");
        }
        if (!StringUtils.hasText(notificationId)) {
            throw new IllegalArgumentException("notificationId is required");
        }

        accessControlService.requireUserSelfOrPrivileged(userId);
        int affectedRows = notificationRepository.deleteByIdAndReceiverId(notificationId, userId);
        if (affectedRows == 0) {
            throw new ResourceNotFoundException("Notification not found: " + notificationId);
        }
    }

    public int clearAllNotifications(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("userId is required");
        }

        accessControlService.requireUserSelfOrPrivileged(userId);
        return notificationRepository.deleteAllByReceiverId(userId);
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .directUrl(notification.getDirectUrl())
                .type(notification.getType())
                .isRead(notification.isRead())
                .readAt(notification.getReadAt())
                .timeCreated(notification.getTimeCreated())
                .expiry(notification.getExpiry())
                .senderId(notification.getSender() != null ? notification.getSender().getId() : null)
                .receiverId(notification.getReceiver() != null ? notification.getReceiver().getId() : null)
                .build();
    }
}
