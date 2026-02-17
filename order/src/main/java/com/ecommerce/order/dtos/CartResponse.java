package com.ecommerce.order.dtos;

import com.ecommerce.order.dtos.CartItemsResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartResponse {
    private String userId;
    private List<CartItemsResponse> items;
    private BigDecimal cartTotal;
}
