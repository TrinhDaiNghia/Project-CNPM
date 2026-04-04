package com.example.demo.dtos.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelOrderRequest {

    @Size(max = 120, message = "Reason must not exceed 120 characters")
    private String reason;

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;
}
