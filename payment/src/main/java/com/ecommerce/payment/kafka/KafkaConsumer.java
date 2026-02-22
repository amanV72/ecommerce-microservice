package com.ecommerce.payment.kafka;

import com.ecommerce.payment.dto.eventDto.InventoryReservedEvent;
import com.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {
    private final PaymentService paymentService;

    @Bean
    public Consumer<InventoryReservedEvent> inventoryReserved(){
        return event ->{
            log.info("Received order is: {} with userId: {}",event.getOrderId(),event.getUserId());
            paymentService.createOrder(event);
        };
    }
}
