package com.ecommerce.payment.repo;

import com.ecommerce.payment.model.Payments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentsRepo extends JpaRepository<Payments,Long> {
}
