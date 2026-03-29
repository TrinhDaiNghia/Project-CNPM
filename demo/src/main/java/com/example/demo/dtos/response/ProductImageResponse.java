package com.example.demo.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductImageResponse {

    private String id;
    private String imageUrl;
    private String altText;
    private Boolean isThumbnail;
}

