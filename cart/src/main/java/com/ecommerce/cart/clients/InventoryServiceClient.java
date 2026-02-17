package com.ecommerce.cart.clients;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface InventoryServiceClient {
    @GetExchange("/api/inventory/{productId}/availability")
    Boolean hasSufficientStock(@PathVariable String productId,
                               @RequestParam("qty") Integer quantity
    );
}
