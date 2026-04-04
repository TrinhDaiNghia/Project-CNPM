package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductDiscussionAskResponse {

    private ProductDiscussionMessageResponse question;
    private ProductDiscussionMessageResponse answer;
}

