package com.example.demo.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.Date;

@Entity
@Table(name = "shippings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipping {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    @Size(max = 100, message = "Tracking number must not exceed 100 characters")
    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "tracking_date")
    private Date trackingDate;

    @Size(max = 100, message = "Carrier name must not exceed 100 characters")
    @Column(name = "carrier_name", length = 100)
    private String carrierName;

    @Pattern(regexp = "^(\\+84|0)[3-9]\\d{8}$", message = "Invalid Vietnamese phone number")
    @Column(name = "carrier_phone", length = 15)
    private String carrierPhone;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "estimated_delivery")
    private Date estimatedDelivery;

    @NotNull(message = "Order is required")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    @JsonIgnore
    private Order order;

    public String getTrackingCode() {
        return trackingNumber;
    }

    public void setTrackingCode(String trackingCode) {
        this.trackingNumber = trackingCode;
    }
}
