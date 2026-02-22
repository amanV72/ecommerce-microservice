package com.ecommerce.inventory.dto.eventDto;

import com.ecommerce.inventory.dto.OrderItemDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEventDto {
    private Long orderId;
    private String userId;
    private Long totalAmount;
    private List<OrderItemDTO> items;
    private LocalDateTime createdAt;
}
