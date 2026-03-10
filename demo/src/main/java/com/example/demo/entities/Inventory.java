package com.example.demo.entities;

import com.example.demo.entities.enums.InventoryStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.Date;

@Entity
@Table(name = "inventories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name = "issue_description", columnDefinition = "TEXT")
    private String issueDescription;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "received_date")
    private Date receivedDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expected_return_date")
    private Date expectedReturnDate;

    @NotNull(message = "Inventory status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private InventoryStatus status = InventoryStatus.PENDING;

    @Size(max = 1000, message = "Tech notes must not exceed 1000 characters")
    @Column(name = "tech_notes", length = 1000)
    private String techNotes;

    @Min(value = 0, message = "Quantity must not be negative")
    @Column(name = "quantity", columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer quantity = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @NotNull(message = "Owner is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;
}
