package com.ecommerce.payment.controller;

import com.ecommerce.payment.dto.RazorpayOrderDetails;
import com.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping("/{paymentId}")
    public ResponseEntity<RazorpayOrderDetails> orderDetails(
            @RequestHeader("X-User-ID")String userId,
            @PathVariable Long paymentId){
        RazorpayOrderDetails details=paymentService.getRazorpayOrderDetails(paymentId,userId);
        return ResponseEntity.ok(details);
    }

}
