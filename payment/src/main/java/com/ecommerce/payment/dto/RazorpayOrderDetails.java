package com.ecommerce.payment.dto;

import lombok.Data;

@Data
public class RazorpayOrderDetails {
    private String orderId;
    private Long amount;
    private String currency;
    private String apiKey;
}
