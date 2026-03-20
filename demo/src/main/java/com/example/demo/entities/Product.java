package com.example.demo.entities;

import com.example.demo.entities.enums.ProductStatus;
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

    @Size(max = 100, message = "Movement type must not exceed 100 characters")
    @Column(name = "movement_type", length = 100)
    private String movementType;

    @Size(max = 100, message = "Glass material must not exceed 100 characters")
    @Column(name = "glass_material", length = 100)
    private String glassMaterial;

    @Size(max = 100, message = "Water resistance must not exceed 100 characters")
    @Column(name = "water_resistance", length = 100)
    private String waterResistance;

    @Size(max = 100, message = "Face size must not exceed 100 characters")
    @Column(name = "face_size", length = 100)
    private String faceSize;

    @Size(max = 100, message = "Wire material must not exceed 100 characters")
    @Column(name = "wire_material", length = 100)
    private String wireMaterial;

    @Size(max = 100, message = "Wire color must not exceed 100 characters")
    @Column(name = "wire_color", length = 100)
    private String wireColor;

    @Size(max = 100, message = "Case color must not exceed 100 characters")
    @Column(name = "case_color", length = 100)
    private String caseColor;

    @Size(max = 100, message = "Face color must not exceed 100 characters")
    @Column(name = "face_color", length = 100)
    private String faceColor;

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
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Warranty> warranties = new ArrayList<>();
}
