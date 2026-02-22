package com.ecommerce.payment.dto.eventDto;

import lombok.Data;

@Data
public class InventoryReservedEvent {
    private Long orderId;
    private String userId;
    private Long totalAmount;
}
