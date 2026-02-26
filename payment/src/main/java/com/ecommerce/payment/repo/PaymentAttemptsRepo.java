package com.ecommerce.payment.repo;

import com.ecommerce.payment.model.PaymentAttempts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentAttemptsRepo extends JpaRepository<PaymentAttempts,Long> {
    Optional<PaymentAttempts> findByRazorpayOrderId(String orderId);
}
