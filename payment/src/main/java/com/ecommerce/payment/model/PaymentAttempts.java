package com.ecommerce.payment.model;

import jakarta.persistence.*;

@Entity
@Table(name = "payment_attempts")
public class PaymentAttempts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
}
