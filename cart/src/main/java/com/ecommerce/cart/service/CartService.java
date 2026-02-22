package com.ecommerce.cart.service;

import com.ecommerce.cart.clients.InventoryServiceClient;
import com.ecommerce.cart.clients.ProductServiceClient;
import com.ecommerce.cart.dto.*;
import com.ecommerce.cart.model.CartItem;
import com.ecommerce.cart.repository.CartItemRepo;

import io.github.resilience4j.retry.annotation.Retry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
    private final CartItemRepo cartItemRepo;
    private final ProductServiceClient productServiceClient;
    private final InventoryServiceClient inventoryServiceClient;
    int attempt = 0;


    //@CircuitBreaker(name = "productService", fallbackMethod = "addToCartFallBack")
    @Retry(name = "retryBreaker", fallbackMethod = "addToCartFallBack")
    public boolean addToCart(String userId, CartItemRequest request) {
        System.out.println("ATTEMPT COUNT: " + ++attempt);

        //Look for Product
        ProductResponse productResponse = productServiceClient.getProductDetails(String.valueOf(request.getProductId()));
        if (productResponse == null) return false;

        //check inventory
        Boolean isAvailable= inventoryServiceClient.hasSufficientStock(request.getProductId(), request.getQuantity());
        if(!isAvailable) return false;

        CartItem existingItem = cartItemRepo.findByUserIdAndProductId(userId, request.getProductId());

        long unitPrice = productResponse.getPrice(); // paise

        if (existingItem != null) {
            //Update the quantity
            int newQuantity =
                    existingItem.getQuantity() + request.getQuantity();

            existingItem.setQuantity(newQuantity);
            existingItem.setPrice(unitPrice);
            existingItem.setTotalPrice(unitPrice * newQuantity);

            cartItemRepo.save(existingItem);
        } else {
            //Create a new cart item
            CartItem cartItem = new CartItem();
            cartItem.setUserId(userId);
            cartItem.setProductId(request.getProductId());
            cartItem.setPrice(productResponse.getPrice());
            cartItem.setQuantity(request.getQuantity());
            cartItem.setTotalPrice(unitPrice*request.getQuantity());
            cartItemRepo.save(cartItem);

        }
        return true;
    }

    public boolean addToCartFallBack(String userId,
                                     CartItemRequest request,
                                     Exception exception) {
        exception.printStackTrace();
        return false;
    }


    public boolean deleteItemFromCart(String userId, String productId) {

        CartItem cartItem = cartItemRepo.findByUserIdAndProductId(userId, productId);

        if (cartItem != null) {
            cartItemRepo.deleteByUserIdAndProductId(userId, productId);
            return true;
        }

        return false;

    }

    public CartResponse getCart(String userId) {
        List<CartItem> items=cartItemRepo.findByUserId(userId);

        List<CartItemsResponse> cartItemsResponses= items.stream().map(cartItem -> {
            CartItemsResponse cir= new CartItemsResponse();
            cir.setProductId(cartItem.getProductId());
            cir.setQuantity(cartItem.getQuantity());
            cir.setPrice(cartItem.getPrice());
            cir.setTotalPrice(cartItem.getTotalPrice());
            return cir;
        }).toList();

        Long cartTotal= items.stream()
                .map(CartItem::getTotalPrice)
                .reduce(0L, Long::sum);

        CartResponse cartResponse= new CartResponse();
        cartResponse.setUserId(userId);
        cartResponse.setItems(cartItemsResponses);
        cartResponse.setCartTotal(cartTotal);

        return cartResponse;
    }

    public void clearCart(String userId) {
        cartItemRepo.deleteByUserId(userId);
    }
}
