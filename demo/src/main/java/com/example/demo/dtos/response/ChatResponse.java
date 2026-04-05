package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatResponse {

    private String sender;
    private String message;
    private String handledBy;
}

