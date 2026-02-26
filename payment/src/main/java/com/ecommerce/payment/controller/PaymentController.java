package com.ecommerce.payment.controller;

import com.ecommerce.payment.dto.PaymentVerificationRequest;
import com.ecommerce.payment.dto.PaymentVerificationResponse;
import com.ecommerce.payment.dto.RazorpayOrderDetails;
import com.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    @PostMapping("/verify-payment")
    public ResponseEntity<PaymentVerificationResponse> verifyPayment(
            @RequestHeader("X-User-ID")String userId,
            @RequestBody PaymentVerificationRequest paymentVerificationDto){
        boolean verified=paymentService.verifyRazorpayPayment(paymentVerificationDto,userId);
        if (verified) {
            return ResponseEntity.ok(
                    new PaymentVerificationResponse(
                            "PAYMENT VERIFIED",
                            "Payment verified successfully"
                    )
            );
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new PaymentVerificationResponse(
                        "UNAUTHORIZED PAYMENT",
                        "Payment verification failed"
                ));
    }

}
