package com.ecommerce.order.services;

import com.ecommerce.order.clients.CartServiceClient;
import com.ecommerce.order.dtos.CartResponse;
import com.ecommerce.order.dtos.OrderCreatedEventDto;
import com.ecommerce.order.dtos.OrderItemDTO;
import com.ecommerce.order.dtos.OrderResponse;
import com.ecommerce.order.models.OrderStatus;
import com.ecommerce.order.models.Order;
import com.ecommerce.order.models.OrderItem;
import com.ecommerce.order.repositories.OrderRepo;
import lombok.RequiredArgsConstructor;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepo orderRepo;
    private final CartServiceClient cartServiceClient;
//    private final RabbitTemplate rabbitTemplate;
//
//    @Value("${rabbitmq.exchange.name}")
//    private String exchangeName;
//
//    @Value("${rabbitmq.routing.key}")
//    private String routingKey;
//   // private final UserRepo userRepo;
    private final StreamBridge streamBridge;


    public Optional<OrderResponse> createOrder(String userId) {
        /// Validate for cart items (REST call to cart-service)
        CartResponse cartResponse=cartServiceClient.getCartDetails(userId);
        if(cartResponse==null) return Optional.empty();

        /// Total price
        BigDecimal totalAmount = cartResponse.getCartTotal();

        /// Create Order
        Order order = new Order();
        order.setUserId(cartResponse.getUserId());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(totalAmount);
        List<OrderItem> orderItems = cartResponse.getItems().stream()
                .map(item -> new OrderItem(
                        null,
                        item.getProductId(),
                        item.getQuantity(),
                        item.getPrice(),
                        order
                )).toList();
        order.setItems(orderItems);
        Order savedOrder = orderRepo.save(order);

        // Clear the cart
        // cartService.clearCart(userId);

        //publish to RabbitMQ
        OrderCreatedEventDto event=new OrderCreatedEventDto(
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getTotalAmount(),
                savedOrder.getStatus(),
                savedOrder.getItems().stream().map(item -> new OrderItemDTO(
                        item.getId(),
                        item.getProductId(),
                        item.getQuantity(),
                        item.getPrice(),
                        item.getPrice().multiply(new BigDecimal(item.getQuantity()))

                )).toList(),
                savedOrder.getCreatedAt()
        );
//        rabbitTemplate.convertAndSend(exchangeName,
//                routingKey,
//               event);
        streamBridge.send("createOrder-out-0",event);


        return Optional.of(orderToOrderResponse(savedOrder));

    }

    private OrderResponse orderToOrderResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getItems().stream().map(item -> new OrderItemDTO(
                        item.getId(),
                        item.getProductId(),
                        item.getQuantity(),
                        item.getPrice(),
                        item.getPrice().multiply(new BigDecimal(item.getQuantity()))
                )).toList(),
                order.getCreatedAt()
        );
    }
}
