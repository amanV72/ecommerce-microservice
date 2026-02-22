package com.ecommerce.payment.dto.eventDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentCreatedEvent {
    private Long orderId;
    private Long paymentId;

}
