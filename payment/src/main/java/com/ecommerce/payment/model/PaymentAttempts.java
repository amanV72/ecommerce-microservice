package com.ecommerce.payment.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_attempts")
@Data
public class PaymentAttempts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String razorpayOrderId;

    @Column(unique = true)
    private  String razorpayPaymentId;

    private Long totalAmount;

    private String currency;

    @Enumerated(EnumType.STRING)
    private AttemptStatus status;

    private String failureReason;

    @ManyToOne
    @JoinColumn(name = "payment_id",nullable = false)
    private Payments payment;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
