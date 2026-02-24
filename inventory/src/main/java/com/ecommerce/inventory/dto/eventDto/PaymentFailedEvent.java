package com.ecommerce.inventory.dto.eventDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentFailedEvent {
    private Long orderId;
    private String userId;
    private String reason;

}
