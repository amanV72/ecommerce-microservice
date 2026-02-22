package com.ecommerce.order.dtos.eventDto;

import com.ecommerce.order.dtos.OrderItemDTO;
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
