package com.ecommerce.order.dtos;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemsResponse {
    private Long productId;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
}
