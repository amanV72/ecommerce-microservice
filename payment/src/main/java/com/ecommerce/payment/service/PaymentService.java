package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.eventDto.InventoryReservedEvent;
import com.ecommerce.payment.dto.eventDto.PaymentCreatedEvent;
import com.ecommerce.payment.dto.RazorpayOrderDetails;
import com.ecommerce.payment.dto.eventDto.PaymentFailedEvent;
import com.ecommerce.payment.model.AttemptStatus;
import com.ecommerce.payment.model.PaymentAttempts;
import com.ecommerce.payment.model.PaymentStatus;
import com.ecommerce.payment.model.Payments;
import com.ecommerce.payment.repo.PaymentsRepo;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    @Value("${razorpay.api.key}")
    private  String apiKey;

    @Value("${razorpay.api.secret}")
    private  String apiSecret;

    private final StreamBridge streamBridge;

    private final PaymentsRepo paymentsRepo;

    @Transactional
    public void createOrder(InventoryReservedEvent event) {
        try {
            RazorpayClient razorpayClient = new RazorpayClient(apiKey, apiSecret);
            JSONObject object = new JSONObject();
            object.put("amount", event.getTotalAmount());
            object.put("currency", "INR");
            object.put("receipt", "order_" + event.getOrderId());

            Order order = razorpayClient.orders.create(object);
            log.info("Created order is: " + order.get("id"));

            Payments existing = paymentsRepo.findByOrderId(event.getOrderId());

            if (existing != null) {
                log.info("Payment already exists for order {}", event.getOrderId());
                streamBridge.send("paymentCreated-out-0", new PaymentCreatedEvent(
                        existing.getOrderId(),
                        existing.getId()
                ));
                return;
            }

            Payments payments= new Payments();
            payments.setOrderId(event.getOrderId());
            payments.setUserId(event.getUserId());
            payments.setTotalAmount(event.getTotalAmount());
            payments.setCurrency("INR");
            payments.setStatus(PaymentStatus.CREATED);

            PaymentAttempts attempts= new PaymentAttempts();
            attempts.setRazorpayOrderId(order.get("id"));
            attempts.setTotalAmount(event.getTotalAmount());
            attempts.setCurrency("INR");
            attempts.setStatus(AttemptStatus.CREATED);

            attempts.setPayment(payments);
            payments.setPaymentAttempts(new ArrayList<>());
            payments.getPaymentAttempts().add(attempts);

            Payments newPayment=paymentsRepo.save(payments);

            streamBridge.send("paymentCreated-out-0", new PaymentCreatedEvent(
                    event.getOrderId(),
                    newPayment.getId()
            ));

        } catch (RazorpayException e) {
           streamBridge.send("paymentFailed-out-0", new PaymentFailedEvent(
                   event.getOrderId(),
                   event.getUserId(),
                   "PAYMENT FAILED"
           ));
        }

    }

    public RazorpayOrderDetails getRazorpayOrderDetails(Long paymentId,String userId) {
        log.info("Received user id is: {} with paymentID: {}",userId,paymentId);
        Payments payments= paymentsRepo.findById(paymentId).orElseThrow();

        if(!payments.getUserId().equals(userId)) throw new RuntimeException("Unauthorized access");


        PaymentAttempts latestAttempt= payments.getPaymentAttempts().get(payments.getPaymentAttempts().size() - 1);

        String razorpayOrderId= latestAttempt.getRazorpayOrderId();

        RazorpayOrderDetails orderDetails= new RazorpayOrderDetails();

        orderDetails.setOrderId(razorpayOrderId);
        orderDetails.setAmount(payments.getTotalAmount());
        orderDetails.setCurrency(payments.getCurrency());
        orderDetails.setApiKey(apiKey);

        return orderDetails;

    }
}
