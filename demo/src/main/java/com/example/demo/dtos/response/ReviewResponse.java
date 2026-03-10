package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ReviewResponse {

    private String id;
    private Integer rating;
    private String comment;
    private Date createdAt;
    private String customerId;
    private String customerUsername;
    private String productId;
    private String productName;
}
