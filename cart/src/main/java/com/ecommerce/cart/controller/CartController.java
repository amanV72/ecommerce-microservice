package com.ecommerce.cart.controller;


import com.ecommerce.cart.dto.CartItemRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.model.CartItem;
import com.ecommerce.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getFromCart(@RequestHeader("X-User-ID") String userId) {

        return ResponseEntity.ok(cartService.getCart(userId));

    }

    @PostMapping
    public ResponseEntity<String> addToCart(@RequestHeader("X-User-ID") String userId,
                                            @RequestBody CartItemRequest request) {

        log.info("User ID is : {}",userId);

        boolean isAdded = cartService.addToCart(userId, request);

        if (!isAdded) return ResponseEntity.badRequest().body("Product Out of Stock or User Not Found!");

        return ResponseEntity.status(HttpStatus.CREATED).build();

    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> deleteFromCart(@RequestHeader("X-User-ID") String userId,
                                               @PathVariable String productId) {
        boolean isDeleted = cartService.deleteItemFromCart(userId, productId);

        return isDeleted ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();

    }
}
