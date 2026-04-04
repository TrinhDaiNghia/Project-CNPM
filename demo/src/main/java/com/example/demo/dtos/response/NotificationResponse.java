package com.example.demo.dtos.response;

import com.example.demo.entities.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class NotificationResponse {

    private String id;
    private String title;
    private String content;
    private String directUrl;
    private NotificationType type;
    private boolean isRead;
    private Date readAt;
    private Date timeCreated;
    private Date expiry;
    private String senderId;
    private String receiverId;
}
