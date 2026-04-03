package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class SupportDiscussionResponse {

    private String id;
    private String customerId;
    private String customerName;
    private Date startDate;
    private Date endDate;
    private String contentLog;
    private Boolean aiHandled;
}

