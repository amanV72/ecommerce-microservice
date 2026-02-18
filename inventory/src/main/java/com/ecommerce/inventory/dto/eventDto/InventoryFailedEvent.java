package com.ecommerce.inventory.dto.eventDto;

import com.ecommerce.inventory.dto.OrderItemDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class InventoryFailedEvent {
    private Long orderId;
    private List<OrderItemDTO> orderItems;
    private String reason;
}
