package com.ecommerce.inventory.kafka;

import com.ecommerce.inventory.kafka.dto.ProductEventResponse;
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
    Consumer<ProductEventResponse> getProductId(){
        return event-> {
            log.info("Received product is: {}",event.getProductId());
            inventoryService.createInventory(event.getProductId());
        };
    }
}
