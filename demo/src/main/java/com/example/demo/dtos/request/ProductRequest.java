package com.example.demo.dtos.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ProductRequest {

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

    @Size(max = 50)
    private String partNumber;

    @Size(max = 100)
    private String powerSource;

    @Size(max = 100)
    private String license;

    @Size(max = 200)
    private String warranty;
}
