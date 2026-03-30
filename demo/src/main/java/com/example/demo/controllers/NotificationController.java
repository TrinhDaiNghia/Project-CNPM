package com.example.demo.controllers;

import com.example.demo.dtos.response.NotificationResponse;
import com.example.demo.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/me")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(@RequestParam String userId) {
        return ResponseEntity.ok(notificationService.getMyNotifications(userId));
    }

}
