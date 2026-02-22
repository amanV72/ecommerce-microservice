package com.ecommerce.order.controllers;


import com.ecommerce.order.dtos.OrderResponse;
import com.ecommerce.order.dtos.OrderResponseForPaymentId;
import com.ecommerce.order.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestHeader("X-User-ID") String userId){
       return orderService.createOrder(userId)
               .map(orderResponse -> new ResponseEntity<>(orderResponse,HttpStatus.CREATED))
               .orElseGet(()->ResponseEntity.badRequest().build());

    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseForPaymentId> getPaymentId(
            @RequestHeader("X-User-ID") String userId,
            @PathVariable Long orderId
    ){
        return orderService.getPaymentIdFromOrder(userId,orderId)
                .map(orderResponse -> new ResponseEntity<>(orderResponse,HttpStatus.CREATED))
                .orElseGet(()->ResponseEntity.badRequest().build());

    }

}
