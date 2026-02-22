package com.ecommerce.cart.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private Long price;
    private String category;
    private String imageUrl;
    private Boolean active;
}
