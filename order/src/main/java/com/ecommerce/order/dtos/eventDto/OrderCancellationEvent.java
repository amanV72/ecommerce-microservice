package com.ecommerce.order.dtos.eventDto;

public interface OrderCancellationEvent {
     Long getOrderId();
     String getReason();
}
