package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.model.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryReservationRepo extends JpaRepository<InventoryReservation,Long> {
    Optional<List<InventoryReservation>> findByOrderId(Long orderId);
}
