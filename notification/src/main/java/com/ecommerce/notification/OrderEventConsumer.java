package com.ecommerce.notification;

import com.ecommerce.notification.payload.OrderCreatedEventDto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Consumer;

@Service
@Slf4j
public class OrderEventConsumer {

//    @RabbitListener(queues = "${rabbitmq.queue.name}")
//    public void handleOrderEvent(OrderCreatedEventDto orderEvent) {
//
//        long orderId = orderEvent.getOrderId();
//        OrderStatus status = orderEvent.getStatus();
//        System.out.println("Order ID: " + orderId);
//        System.out.println("Order Status: " + status);
//
//
//    }
    @Bean
    public Consumer<OrderCreatedEventDto> orderSuccess(){
        return event ->{
            log.info("Received order event order ID: {}",event.getOrderId());
            log.info("Received order event user ID: {}",event.getUserId());

        };
    }
}
