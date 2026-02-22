package com.ecommerce.payment.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "payments")
@Data
public class Payments {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true,nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private String userId;

    private Long totalAmount;

    private String currency;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL,orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<PaymentAttempts> paymentAttempts;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
