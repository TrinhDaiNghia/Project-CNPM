package com.example.demo.entities;

import com.example.demo.entities.enums.WarrantyStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.util.Date;

@Entity
@Table(name = "warranties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warranty {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    @NotNull(message = "Customer is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotNull(message = "Order is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotNull(message = "Product is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotBlank(message = "Issue description is required")
    @Size(max = 1000, message = "Issue description must not exceed 1000 characters")
    @Column(name = "issue_description", nullable = false, length = 1000)
    private String issueDescription;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @NotNull(message = "Warranty status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private WarrantyStatus status = WarrantyStatus.PENDING;

    @Size(max = 1000, message = "Resolution note must not exceed 1000 characters")
    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "requested_at", updatable = false)
    private Date requestedAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "processed_at")
    private Date processedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;
}
