package com.ecommerce.order.dtos.eventDto;

import com.ecommerce.order.dtos.OrderItemDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class InventoryFailedEvent implements OrderCancellationEvent{
    private Long orderId;
    private String reason;
}
