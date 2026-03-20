package com.example.demo.dtos.response;

import com.example.demo.entities.enums.ProductStatus;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class ProductResponse {

    private String id;
    private String brand;
    private String name;
    private String description;
    private Long price;
    private Integer stockQuantity;
    private String movementType;
    private String glassMaterial;
    private String waterResistance;
    private String faceSize;
    private String wireMaterial;
    private String wireColor;
    private String caseColor;
    private String faceColor;
    private ProductStatus status;
    private String categoryId;
    private String categoryName;
    private List<String> imageUrls;
    private Double averageRating;
    private Date updatedAt;
}
