package com.ecommerce.inventory.kafka;

import com.ecommerce.inventory.dto.eventDto.OrderCreatedEventDto;
import com.ecommerce.inventory.dto.eventDto.PaymentFailedEvent;
import com.ecommerce.inventory.dto.eventDto.PaymentSuccessEvent;
import com.ecommerce.inventory.dto.eventDto.ProductCreatedEvent;
import com.ecommerce.inventory.services.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {
    private final InventoryService inventoryService;
    @Bean
    Consumer<ProductCreatedEvent> initializeProduct(){
        return event-> {
            log.info("Received product is: {}",event.getProductId());
            inventoryService.initializeInventory(event.getProductId());
        };
    }
    @Bean
    Consumer<OrderCreatedEventDto> orderCreated(){
        return event-> {
            log.info("Received order is: {}",event.getOrderId());
            inventoryService.hasSufficientStockForEvent(event);
        };
    }

    @Bean
    Consumer<PaymentFailedEvent> paymentFailed(){
        return event-> {
            log.info("Payment Failed order is: {}",event.getOrderId());
            inventoryService.revertInventory(event);
        };
    }

    @Bean
    Consumer<PaymentSuccessEvent> paymentSuccess(){
        return event ->{
            log.info("Payment Success order is: {}",event.getOrderId());
            inventoryService.handlePaymentSuccess(event);
        };
    }
}
