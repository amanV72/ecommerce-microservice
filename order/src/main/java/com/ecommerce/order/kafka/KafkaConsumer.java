package com.ecommerce.order.kafka;

import com.ecommerce.order.dtos.eventDto.InventoryFailedEvent;
import com.ecommerce.order.dtos.eventDto.InventoryRevertedEvent;
import com.ecommerce.order.dtos.eventDto.PaymentCreatedEvent;
import com.ecommerce.order.dtos.eventDto.PaymentSuccessEvent;
import com.ecommerce.order.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {
    private final OrderService orderService;

    @Bean
    Consumer<InventoryFailedEvent> inventoryFailed(){
        return event ->{
            log.info("Inventory is failed because: {}",event.getReason());
            orderService.cancelOrder(event);
        };
    }
    @Bean
    Consumer<PaymentCreatedEvent> paymentCreated(){
        return event->{
            log.info("Payment Id is: {}",event.getPaymentId());
            orderService.attachPaymentIdToModel(event);
        };
    }

    @Bean
    Consumer<InventoryRevertedEvent> inventoryReverted(){
        return event ->{
            log.info("Order id: {} is cancelled because {}",event.getOrderId(),event.getReason());
            orderService.cancelOrder(event);
        };
    }

    @Bean
    Consumer<PaymentSuccessEvent> paymentSuccess(){
        return event ->{
            log.info("Payment Success order is: {}",event.getOrderId());
            orderService.handlePaymentSuccess(event);
        };
    }
}
