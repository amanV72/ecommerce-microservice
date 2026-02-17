package com.ecommerce.cart.dto;

import lombok.Data;

@Data
public class CartItemRequest {

    private String productId;
    private Integer quantity;
}
