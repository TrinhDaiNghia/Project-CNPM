package com.example.demo.controllers;

import com.example.demo.dtos.response.NotificationResponse;
import com.example.demo.entities.enums.NotificationType;
import com.example.demo.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @RequestParam String userId,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) String type
    ) {
        return ResponseEntity.ok(
                notificationService.getNotificationsByUserId(userId, isRead, resolveType(type))
        );
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByUser(
            @PathVariable String userId,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) String type
    ) {
        return ResponseEntity.ok(
                notificationService.getNotificationsByUserId(userId, isRead, resolveType(type))
        );
    }

    @GetMapping("/receiver/{userId}")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByReceiver(
            @PathVariable String userId,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) String type
    ) {
        return ResponseEntity.ok(
                notificationService.getNotificationsByUserId(userId, isRead, resolveType(type))
        );
    }

    @GetMapping("/me")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @RequestParam String userId,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) String type
    ) {
        return ResponseEntity.ok(notificationService.getMyNotifications(userId, isRead, resolveType(type)));
    }

    @PatchMapping({"/{notificationId}/read", "/read/{notificationId}"})
    public ResponseEntity<Void> markAsRead(
            @PathVariable String notificationId,
            @RequestParam(required = false) String userId,
            @RequestBody(required = false) Map<String, String> payload
    ) {
        notificationService.markAsRead(resolveUserId(null, userId, payload), notificationId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping({"/{notificationId}/read", "/read/{notificationId}"})
    public ResponseEntity<Void> markAsReadByPut(
            @PathVariable String notificationId,
            @RequestParam(required = false) String userId,
            @RequestBody(required = false) Map<String, String> payload
    ) {
        notificationService.markAsRead(resolveUserId(null, userId, payload), notificationId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping({"/user/{userId}/read-all", "/read-all"})
    public ResponseEntity<Void> markAllAsRead(
            @PathVariable(required = false) String userId,
            @RequestParam(required = false) String queryUserId,
            @RequestBody(required = false) Map<String, String> payload
    ) {
        notificationService.markAllAsRead(resolveUserId(userId, queryUserId, payload));
        return ResponseEntity.noContent().build();
    }

    @PutMapping({"/user/{userId}/read-all", "/read-all"})
    public ResponseEntity<Void> markAllAsReadByPut(
            @PathVariable(required = false) String userId,
            @RequestParam(required = false) String queryUserId,
            @RequestBody(required = false) Map<String, String> payload
    ) {
        notificationService.markAllAsRead(resolveUserId(userId, queryUserId, payload));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping({"/{notificationId}", "/delete/{notificationId}"})
    public ResponseEntity<Void> deleteNotification(
            @PathVariable String notificationId,
            @RequestParam(required = false) String userId,
            @RequestBody(required = false) Map<String, String> payload
    ) {
        notificationService.deleteNotification(resolveUserId(null, userId, payload), notificationId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping({"/user/{userId}", "/clear-all"})
    public ResponseEntity<Void> clearAllNotifications(
            @PathVariable(required = false) String userId,
            @RequestParam(required = false) String queryUserId,
            @RequestBody(required = false) Map<String, String> payload
    ) {
        notificationService.clearAllNotifications(resolveUserId(userId, queryUserId, payload));
        return ResponseEntity.noContent().build();
    }

    private String resolveUserId(String pathUserId, String queryUserId, Map<String, String> payload) {
        if (StringUtils.hasText(pathUserId)) {
            return pathUserId;
        }
        if (StringUtils.hasText(queryUserId)) {
            return queryUserId;
        }
        if (payload != null) {
            String bodyUserId = payload.get("userId");
            if (StringUtils.hasText(bodyUserId)) {
                return bodyUserId;
            }
        }
        throw new IllegalArgumentException("userId is required");
    }

    private NotificationType resolveType(String type) {
        if (!StringUtils.hasText(type)) {
            return null;
        }
        try {
            return NotificationType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid notification type: " + type);
        }
    }
}
