package com.example.demo.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
    name = "order_items",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_order_product",
        columnNames = {"order_id", "product_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Min(value = 0, message = "Sub total must not be negative")
    @Column(name = "sub_total")
    private Long subTotal;

    @NotNull(message = "Order is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    @NotNull(message = "Product is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}
