package com.ecommerce.inventory.dto.eventDto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class InventoryReservedEvent {
    private Long orderId;
    private String userId;
    private Long totalAmount;
}
