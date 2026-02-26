package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.PaymentVerificationRequest;
import com.ecommerce.payment.dto.eventDto.InventoryReservedEvent;
import com.ecommerce.payment.dto.eventDto.PaymentCreatedEvent;
import com.ecommerce.payment.dto.RazorpayOrderDetails;
import com.ecommerce.payment.dto.eventDto.PaymentFailedEvent;
import com.ecommerce.payment.dto.eventDto.PaymentSuccessEvent;
import com.ecommerce.payment.model.AttemptStatus;
import com.ecommerce.payment.model.PaymentAttempts;
import com.ecommerce.payment.model.PaymentStatus;
import com.ecommerce.payment.model.Payments;
import com.ecommerce.payment.repo.PaymentAttemptsRepo;
import com.ecommerce.payment.repo.PaymentsRepo;
import com.razorpay.*;
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

    private final PaymentAttemptsRepo paymentAttemptsRepo;

    @Transactional
    public void createOrder(InventoryReservedEvent event) {
        try {
//            if(event.isSimulateFailure()) {
//                log.error("Payment failing......");
//                throw new RazorpayException("Payment failure simulation");
//            }
            RazorpayClient razorpayClient = new RazorpayClient(apiKey, apiSecret);
            JSONObject object = new JSONObject();
            object.put("amount", event.getTotalAmount());
            object.put("currency", "INR");
            object.put("receipt", "order_" + event.getOrderId());

            Payments existing = paymentsRepo.findByOrderId(event.getOrderId());

            if (existing != null) {
                log.info("Payment already exists for order {}", event.getOrderId());
                streamBridge.send("paymentCreated-out-0", new PaymentCreatedEvent(
                        existing.getOrderId(),
                        existing.getId()
                ));
                return;
            }
            Order order = razorpayClient.orders.create(object);
            log.info("Created order is: " + order.get("id"));

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

    @Transactional
    public boolean verifyRazorpayPayment(PaymentVerificationRequest dto,String userId) {
        JSONObject payment = new JSONObject();
        payment.put("razorpay_order_id", dto.getOrderId());
        payment.put("razorpay_payment_id", dto.getPaymentId());
        payment.put("razorpay_signature", dto.getSignature());
        try {
           boolean verified = Utils.verifyPaymentSignature(payment, apiSecret);
           if(!verified) return false;


           PaymentAttempts attempts=paymentAttemptsRepo.findByRazorpayOrderId(dto.getOrderId()).orElseThrow();
           Payments model= attempts.getPayment();
           if(!model.getUserId().equals(userId)) return false;

           if (attempts.getStatus()==AttemptStatus.SUCCESS) return true;

           RazorpayClient client= new RazorpayClient(apiKey,apiSecret);
           Payment razorpayPayment= client.payments.fetch(dto.getPaymentId());

           String status = razorpayPayment.get("status");
           if (!"captured".equals(status)) {
                return false;
            }

            Number amountNumber = razorpayPayment.get("amount");
            Long razorpayAmount = amountNumber.longValue();
            if(!razorpayAmount.equals(model.getTotalAmount())) return false;

           attempts.setRazorpayPaymentId(dto.getPaymentId());
           attempts.setStatus(AttemptStatus.SUCCESS);


           model.setStatus(PaymentStatus.SUCCESS);

            streamBridge.send("paymentSuccess-out-0",
                    new PaymentSuccessEvent(
                            model.getOrderId(),
                            model.getId(),
                            model.getUserId()
                    )
            );
            return true;

        } catch (RazorpayException e) {
            return false;
        }
    }
}
