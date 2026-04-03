package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatHistoryMessageResponse {

    private String id;
    private String role;
    private String content;
    private String createdAt;
    private String handledBy;
}

