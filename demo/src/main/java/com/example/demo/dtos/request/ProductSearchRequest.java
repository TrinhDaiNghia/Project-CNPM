package com.example.demo.dtos.request;

import com.example.demo.entities.enums.ProductStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductSearchRequest {

    @Size(max = 200)
    private String name;

    @Size(max = 100)
    private String brand;

    @Size(max = 100)
    private String color;

    @Size(max = 100)
    private String size;

    @Size(max = 1000)
    private String spec;

    private ProductStatus status;
}

