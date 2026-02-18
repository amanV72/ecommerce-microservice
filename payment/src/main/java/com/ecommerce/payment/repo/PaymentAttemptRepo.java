package com.ecommerce.payment.repo;

import com.ecommerce.payment.model.PaymentAttempts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentAttemptRepo extends JpaRepository<PaymentAttempts,Long> {
}
