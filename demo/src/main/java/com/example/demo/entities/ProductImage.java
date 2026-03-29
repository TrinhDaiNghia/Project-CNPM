package com.example.demo.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    @NotBlank(message = "Image URL is required")
    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @NotBlank(message = "Cloudinary public id is required")
    @Size(max = 255, message = "Public id must not exceed 255 characters")
    @Column(name = "public_id", nullable = false, length = 255, unique = true)
    private String publicId;

    @Size(max = 255, message = "Alt text must not exceed 255 characters")
    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "is_thumbnail", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean isThumbnail = false;

    @NotNull(message = "Product is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;
}
