package com.example.demo.entities;

import com.example.demo.entities.enums.VoucherStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    @NotBlank(message = "Voucher code is required")
    @Size(min = 4, max = 50, message = "Voucher code must be between 4 and 50 characters")
    @Column(name = "voucher_code", nullable = false, unique = true, length = 50)
    private String voucherCode;

    @Min(value = 0, message = "Discount percent must not be negative")
    @Max(value = 100, message = "Discount percent must not exceed 100")
    @Column(name = "discount_percent")
    @Builder.Default
    private Integer discountPercent = 0;

    @Min(value = 0, message = "Min order amount must not be negative")
    @Column(name = "min_order_amount")
    @Builder.Default
    private Long minOrderAmount = 0L;

    @NotNull(message = "Valid from date is required")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "valid_from", nullable = false)
    private Date validFrom;

    @NotNull(message = "Valid to date is required")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "valid_to", nullable = false)
    private Date validTo;

    @Min(value = 1, message = "Max usage must be at least 1")
    @Column(name = "max_usage", nullable = false)
    @Builder.Default
    private Integer maxUsage = 1;

    @Min(value = 0, message = "Used count must not be negative")
    @Column(name = "used_count", columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer usedCount = 0;

    @NotNull(message = "Voucher status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private VoucherStatus status = VoucherStatus.ACTIVE;

    @OneToMany(mappedBy = "voucher", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();
}
