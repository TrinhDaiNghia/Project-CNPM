package com.example.demo.dtos.response;

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
    private Date timeCreated;
    private Date expiry;
    private String senderId;
    private String receiverId;
}
