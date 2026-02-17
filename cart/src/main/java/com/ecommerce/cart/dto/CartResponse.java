package com.ecommerce.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartResponse {
    private String userId;
    private List<CartItemsResponse> items;
    private BigDecimal cartTotal;
}
