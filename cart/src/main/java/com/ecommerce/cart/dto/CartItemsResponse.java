package com.ecommerce.cart.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemsResponse {
    private String productId;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
}
