package com.example.demo.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.util.Date;

@Entity
@Table(
    name = "reviews",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_customer_product_review",
        columnNames = {"customer_id", "product_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must not exceed 5")
    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    @Column(name = "comment", length = 1000)
    private String comment;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", updatable = false)
    private Date createdAt;

    @NotNull(message = "Customer is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    @NotNull(message = "Product is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;
}
