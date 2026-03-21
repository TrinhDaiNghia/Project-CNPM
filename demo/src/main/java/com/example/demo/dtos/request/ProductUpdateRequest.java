package com.example.demo.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductUpdateRequest {

    @NotBlank(message = "Brand is required")
    @Size(max = 100)
    private String brand;

    @NotBlank(message = "Product name is required")
    @Size(max = 200)
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must not be negative")
    private Long price;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0)
    private Integer stockQuantity;

    @NotBlank(message = "Category ID is required")
    private String categoryId;

    @Size(max = 100)
    private String movementType;

    @Size(max = 100)
    private String glassMaterial;

    @Size(max = 100)
    private String waterResistance;

    @Size(max = 100)
    private String faceSize;

    @Size(max = 100)
    private String wireMaterial;

    @Size(max = 100)
    private String wireColor;

    @Size(max = 100)
    private String caseColor;

    @Size(max = 100)
    private String faceColor;

    @Size(max = 100)
    private String color;

    @Size(max = 100)
    private String size;

    @Size(max = 1000)
    private String specs;
}

