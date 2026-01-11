package com.ecommerce.order.services;

import com.ecommerce.order.clients.ProductServiceClient;
import com.ecommerce.order.clients.UserServiceClient;
import com.ecommerce.order.dtos.CartItemRequest;
import com.ecommerce.order.dtos.ProductResponse;
import com.ecommerce.order.dtos.UserResponse;
import com.ecommerce.order.models.CartItem;
import com.ecommerce.order.repositories.CartItemRepo;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
    private final CartItemRepo cartItemRepo;
    private final ProductServiceClient productServiceClient;
    private final UserServiceClient userServiceClient;
    int attempt = 0;
//    private final ProductRepo productRepo;
//    private final UserRepo userRepo;


    //    @CircuitBreaker(name = "productService", fallbackMethod = "addToCartFallBack")
    @Retry(name = "retryBreaker", fallbackMethod = "addToCartFallBack")
    public boolean addToCart(String userId, CartItemRequest request) {
        System.out.println("ATTEMPT COUNT: " + ++attempt);
        //Look for Product
        ProductResponse productResponse = productServiceClient.getProductDetails(String.valueOf(request.getProductId()));
        if (productResponse == null) return false;

        if (productResponse.getStockQuantity() < request.getQuantity()) return false;
//
//        //Look for User
        UserResponse userResponse = userServiceClient.getUserDetails(userId);
        if (userResponse == null) return false;

        CartItem existingItem = cartItemRepo.findByUserIdAndProductId(userId, request.getProductId());

        if (existingItem != null) {
            //Update the quantity
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            existingItem.setPrice(productResponse.getPrice());
            existingItem.setTotalPrice(existingItem.getTotalPrice().add(productResponse.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()))));
            cartItemRepo.save(existingItem);
        } else {
            //Create a new cart item
            CartItem cartItem = new CartItem();
            cartItem.setUserId(userId);
            cartItem.setProductId(request.getProductId());
            cartItem.setPrice(productResponse.getPrice());
            cartItem.setQuantity(request.getQuantity());
            cartItem.setTotalPrice(productResponse.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
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


    public boolean deleteItemFromCart(String userId, Long productId) {

//        Optional<Product> productOpt = productRepo.findById(productId);
//
//
//        Optional<User> userOpt = userRepo.findById(Long.valueOf(userId));
//
//        if (productOpt.isPresent() && userOpt.isPresent()) {
//            cartItemRepo.deleteByUserIdAndProductId(userOpt.get(), productOpt.get());
//            return true;
//        }

        CartItem cartItem = cartItemRepo.findByUserIdAndProductId(userId, productId);

        if (cartItem != null) {
            cartItemRepo.deleteByUserIdAndProductId(userId, productId);
            return true;
        }

        return false;

    }

    public List<CartItem> getCart(String userId) {
        //System.out.println("Fetched cart for userId=" + userId );
        return cartItemRepo.findByUserId(userId);
    }

    public void clearCart(String userId) {
        cartItemRepo.deleteByUserId(userId);
    }
}
