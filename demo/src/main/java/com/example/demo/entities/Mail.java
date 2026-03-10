package com.example.demo.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.util.Date;

@Entity
@Table(name = "mails")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mail {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "def_date", updatable = false)
    private Date defDate;

    @NotBlank(message = "Subject is required")
    @Size(max = 200, message = "Subject must not exceed 200 characters")
    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Size(max = 255, message = "Recipient must not exceed 255 characters")
    @Column(name = "recipient", length = 255)
    private String recipient;

    @Column(name = "is_sent", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean isSent = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
