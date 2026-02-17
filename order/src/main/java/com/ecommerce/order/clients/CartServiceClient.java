package com.ecommerce.order.clients;

import com.ecommerce.order.dtos.CartResponse;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface CartServiceClient {
    @GetExchange("/api/cart")
    CartResponse getCartDetails(@RequestHeader("X-User-ID") String id);
}
