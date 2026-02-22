package com.ecommerce.order.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class OrderItemDTO {

   // private Long id;
    private Long productId;
    private Integer quantity;
    private Long price;
    private Long totalPrice;


}
