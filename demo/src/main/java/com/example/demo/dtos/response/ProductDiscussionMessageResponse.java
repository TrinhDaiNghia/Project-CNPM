package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductDiscussionMessageResponse {

    private String id;
    private String productId;
    private String userId;
    private String content;
    private String parentId;
    private String role;
    private String handledBy;
    private Boolean aiHandled;
    private String createdAt;
}

