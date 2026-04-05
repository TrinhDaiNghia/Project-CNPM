package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SupplierResponse {

    private String id;
    private String name;
    private String contractInfo;
    private String address;
}

