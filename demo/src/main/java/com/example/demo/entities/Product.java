package com.example.demo.entities;

import com.example.demo.entities.enums.ProductStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    @NotBlank(message = "Brand is required")
    @Size(max = 100, message = "Brand must not exceed 100 characters")
    @Column(name = "brand", nullable = false, length = 100)
    private String brand;

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must not exceed 200 characters")
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must not be negative")
    @Column(name = "price", nullable = false)
    private Long price;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity must not be negative")
    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    @Min(value = 0, message = "Movement quantity must not be negative")
    @Column(name = "movement_qty")
    @Builder.Default
    private Integer movementQty = 0;

    @Size(max = 50, message = "Part number must not exceed 50 characters")
    @Column(name = "part_number", length = 50)
    private String partNumber;

    @Size(max = 100, message = "Power source must not exceed 100 characters")
    @Column(name = "power_source", length = 100)
    private String powerSource;

    @Size(max = 100, message = "License must not exceed 100 characters")
    @Column(name = "license", length = 100)
    private String license;

    @Size(max = 200, message = "Warranty must not exceed 200 characters")
    @Column(name = "warranty", length = 200)
    private String warranty;

    @Min(value = 0, message = "Unit cost must not be negative")
    @Column(name = "unit_cost")
    private Long unitCost;

    @Temporal(TemporalType.DATE)
    @Column(name = "test_date")
    private Date testDate;

    @NotNull(message = "Product status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private Date updatedAt;

    @NotNull(message = "Category is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();
}
