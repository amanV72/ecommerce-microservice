package com.ecommerce.order.dtos.eventDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentCreatedEvent {
    private Long orderId;
    private Long paymentId;

}
