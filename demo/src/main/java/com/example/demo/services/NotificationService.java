package com.example.demo.services;

import com.example.demo.dtos.response.NotificationResponse;
import com.example.demo.entities.Notification;
import com.example.demo.entities.User;
import com.example.demo.entities.enums.UserRole;
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

    private static final String REGISTRATION_SUCCESS_TITLE = "Đăng ký thành công";
    private static final String PASSWORD_RESET_SUCCESS_TITLE = "Đặt lại mật khẩu thành công";
    private static final String ORDER_SUCCESS_TITLE = "Đặt hàng thành công";
    private static final String NEW_ORDER_TO_STORE_TITLE = "Có đơn hàng mới";
    private static final String ORDER_CONFIRMED_TITLE = "Đơn hàng đã được xác nhận";
    private static final String ORDER_CANCELLED_BY_STORE_TITLE = "Đơn hàng đã bị hủy bởi cửa hàng";
    private static final String ORDER_DELIVERED_TITLE = "Đơn hàng đã được giao";
    private static final String ORDER_STATUS_UPDATE_TITLE = "Cập nhật trạng thái đơn hàng";

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

        String content = "Chào " + receiver.getUsername()
                + ", tài khoản của bạn đã được tạo thành công.";

        Notification notification = Notification.builder()
                .title(REGISTRATION_SUCCESS_TITLE)
                .content(content)
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

        String content = "Chào " + receiver.getUsername()
                + ", mật khẩu của bạn đã được đặt lại thành công. Vui lòng đăng nhập lại với mật khẩu mới.";

        Notification notification = Notification.builder()
                .title(PASSWORD_RESET_SUCCESS_TITLE)
                .content(content)
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

        String content = "Chào " + receiver.getUsername()
                + ", đơn hàng " + orderId + " đã được đặt thành công.";

        Notification notification = Notification.builder()
                .title(ORDER_SUCCESS_TITLE)
                .content(content)
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

        String content = "Chào " + receiver.getUsername()
                + ", bạn có một đơn hàng mới với mã " + orderId + ".";

        Notification notification = Notification.builder()
                .title(NEW_ORDER_TO_STORE_TITLE)
                .content(content)
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

        String content = "Chào " + receiver.getUsername()
                + ", đơn hàng " + orderId + " đã được cập nhật trạng thái: " + newStatus + ".";

        Notification notification = Notification.builder()
                .title(title)
                .content(content)
                .sender(sender)
                .receiver(receiver)
                .expiry(Date.from(Instant.now().plus(30, ChronoUnit.DAYS)))
                .build();

        notificationRepository.save(notification);
    }

    public void sendOrderConfirmedNotification(String senderId, User customer, String orderId) {
        sendOrderStatusUpdateNotification(senderId, customer, orderId, ORDER_CONFIRMED_TITLE, "Đã được xác nhận");
    }

    public void sendOrderCancelledByStoreNotification(String senderId, User customer, String orderId) {
        sendOrderStatusUpdateNotification(senderId, customer, orderId, ORDER_CANCELLED_BY_STORE_TITLE, "Đã bị hủy bởi cửa hàng");
    }

    public void sendOrderDeliveredNotification(String senderId, User customer, String orderId) {
        sendOrderStatusUpdateNotification(senderId, customer, orderId, ORDER_DELIVERED_TITLE, "Đã được giao");
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

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .timeCreated(notification.getTimeCreated())
                .expiry(notification.getExpiry())
                .senderId(notification.getSender() != null ? notification.getSender().getId() : null)
                .receiverId(notification.getReceiver() != null ? notification.getReceiver().getId() : null)
                .build();
    }
}

