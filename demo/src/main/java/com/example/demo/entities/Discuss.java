package com.example.demo.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.Date;

@Entity
@Table(name = "discussions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Discuss {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private String id;

    @NotNull(message = "Start date is required")
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "start_date", nullable = false)
    private Date startDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "end_date")
    private Date endDate;

    @Min(value = 0, message = "Score must not be negative")
    @Max(value = 10, message = "Score must not exceed 10")
    @Column(name = "score_sent")
    private Integer scoreSent;

    @Column(name = "content_log", columnDefinition = "TEXT")
    private String contentLog;

    @NotNull(message = "Customer is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;
}
