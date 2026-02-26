package com.ecommerce.order.dtos.eventDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InventoryRevertedEvent implements OrderCancellationEvent{
    private Long orderId;
    private String userId;
    private String reason;
}
