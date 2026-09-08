package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.PaymentVerificationRequest;
import com.ecommerce.payment.dto.RazorpayOrderDetails;
import com.ecommerce.payment.dto.eventDto.InventoryReservedEvent;
import com.ecommerce.payment.dto.eventDto.PaymentCreatedEvent;
import com.ecommerce.payment.dto.eventDto.PaymentFailedEvent;
import com.ecommerce.payment.dto.eventDto.PaymentSuccessEvent;
import com.ecommerce.payment.model.AttemptStatus;
import com.ecommerce.payment.model.PaymentAttempts;
import com.ecommerce.payment.model.PaymentStatus;
import com.ecommerce.payment.model.Payments;
import com.ecommerce.payment.repo.PaymentAttemptsRepo;
import com.ecommerce.payment.repo.PaymentsRepo;
import com.razorpay.*;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock
    private  StreamBridge streamBridge;

    @Mock
    private  PaymentsRepo paymentsRepo;
    @Mock
    private  PaymentAttemptsRepo paymentAttemptsRepo;
    @Mock
    private RazorpayClient razorpayClient;
    @Mock
    private OrderClient orderClient;

    @Mock
    private Order razorpayOrder;
    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private PaymentService paymentService;


    private Payments savedPayments;

    @BeforeEach
    void setUp(){
        ReflectionTestUtils.setField(paymentService,"apiKey","testApiKey");
        ReflectionTestUtils.setField(paymentService,"apiSecret","testApiSecret123");
        ReflectionTestUtils.setField(razorpayClient, "orders", orderClient);
        ReflectionTestUtils.setField(
                razorpayClient,
                "payments",
                paymentClient
        );
                this.savedPayments=Payments.builder()
                        .id(1L)
                .orderId(1234L)
                .userId("testUserId")
                .totalAmount(20000L)
                .currency("INR")
                .status(PaymentStatus.CREATED)
                .build();


    }

    @Nested
    class CreateOrderTests{

        @Test
        void shouldCreateOrderSuccessfully() throws RazorpayException {
            //Given
            InventoryReservedEvent event= InventoryReservedEvent.builder()
                    .orderId(1234L)
                    .userId("testUserId")
                    .totalAmount(20000L)
                    .simulateFailure(false)
                    .build();
            when(paymentsRepo.findByOrderId(event.getOrderId())).thenReturn(null);
            when(razorpayOrder.get("id")).thenReturn("testOrderId");

            when(razorpayClient.orders.create(any(JSONObject.class))).thenReturn(razorpayOrder);

            when(paymentsRepo.save(any(Payments.class))).thenReturn(savedPayments);
            when(streamBridge.send(eq("paymentCreated-out-0"),
                    any(PaymentCreatedEvent.class)
                    )).thenReturn(true);


            //When
            paymentService.createOrder(event);

            //Then

            verify(paymentsRepo).findByOrderId(1234L);

            verify(razorpayClient.orders).create(any(JSONObject.class));

            verify(streamBridge).send(
                    eq("paymentCreated-out-0"),
                    any(PaymentCreatedEvent.class)
            );

            ArgumentCaptor<Payments> paymentCaptor=ArgumentCaptor.forClass(Payments.class);
            verify(paymentsRepo,times(1)).save(paymentCaptor.capture());
            Payments capturedPayment= paymentCaptor.getValue();

            assertEquals(1234L, capturedPayment.getOrderId());
            assertEquals("testUserId", capturedPayment.getUserId());
            assertEquals(20000L, capturedPayment.getTotalAmount());
            assertEquals("INR", capturedPayment.getCurrency());
            assertEquals(PaymentStatus.CREATED, capturedPayment.getStatus());

            assertEquals(1, capturedPayment.getPaymentAttempts().size());

            PaymentAttempts attempt =
                    capturedPayment.getPaymentAttempts().get(0);

            assertEquals("testOrderId", attempt.getRazorpayOrderId());
            assertEquals(20000L, attempt.getTotalAmount());
            assertEquals("INR", attempt.getCurrency());
            assertEquals(AttemptStatus.CREATED, attempt.getStatus());
            assertSame(capturedPayment, attempt.getPayment());
        }

        @Test
        void shouldReturnNothingIfPaymentExists() throws RazorpayException {
            //Given
            InventoryReservedEvent event= InventoryReservedEvent.builder()
                    .orderId(1234L)
                    .userId("testUserId")
                    .totalAmount(20000L)
                    .simulateFailure(false)
                    .build();
            when(paymentsRepo.findByOrderId(event.getOrderId())).thenReturn(savedPayments);

            when(streamBridge.send(eq("paymentCreated-out-0"),
                    any(PaymentCreatedEvent.class)
            )).thenReturn(true);


            //When
            paymentService.createOrder(event);

            //Then

            verify(paymentsRepo,times(1)).findByOrderId(any(Long.class));

            verify(streamBridge,times(1)).send(
                    eq("paymentCreated-out-0"),
                    any(PaymentCreatedEvent.class)
            );
            verify(razorpayClient.orders, never()).create(any(JSONObject.class));

            verify(paymentsRepo, never()).save(any(Payments.class));



        }

        @Test
        void shouldThrowRazorpayExceptionIfOrderNotCreated() throws RazorpayException {
            //Given
            InventoryReservedEvent event= InventoryReservedEvent.builder()
                    .orderId(1234L)
                    .userId("testUserId")
                    .totalAmount(20000L)
                    .simulateFailure(false)
                    .build();
            when(paymentsRepo.findByOrderId(event.getOrderId())).thenReturn(null);

            when(razorpayClient.orders.create(any(JSONObject.class))).thenThrow(new RazorpayException("Order creation failed"));

            when(streamBridge.send(eq("paymentFailed-out-0"),
                    any(PaymentFailedEvent.class)
            )).thenReturn(true);


            //When
            paymentService.createOrder(event);

            //Then
            verify(orderClient,times(1)).create(any(JSONObject.class));

            verify(streamBridge,times(1)).send(
                    eq("paymentFailed-out-0"),
                    any(PaymentFailedEvent.class)
            );

            verify(paymentsRepo, never()).save(any(Payments.class));

            verify(streamBridge, never()).send(
                    eq("paymentCreated-out-0"),
                    any(PaymentCreatedEvent.class)
            );
        }

        @Test
        void shouldThrowExceptionWhenPaymentSaveFails() throws RazorpayException {

            // Given
            InventoryReservedEvent event = InventoryReservedEvent.builder()
                    .orderId(1234L)
                    .userId("testUserId")
                    .totalAmount(20000L)
                    .simulateFailure(false)
                    .build();

            when(paymentsRepo.findByOrderId(event.getOrderId()))
                    .thenReturn(null);

            when(razorpayOrder.get("id"))
                    .thenReturn("testOrderId");

            when(orderClient.create(any(JSONObject.class)))
                    .thenReturn(razorpayOrder);

            when(paymentsRepo.save(any(Payments.class)))
                    .thenThrow(new RuntimeException("Database save failed"));

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> paymentService.createOrder(event)
            );

            assertEquals("Database save failed", exception.getMessage());

            verify(orderClient).create(any(JSONObject.class));

            verify(paymentsRepo).save(any(Payments.class));

            verify(streamBridge, never()).send(
                    eq("paymentCreated-out-0"),
                    any(PaymentCreatedEvent.class)
            );

            verify(streamBridge, never()).send(
                    eq("paymentFailed-out-0"),
                    any(PaymentFailedEvent.class)
            );
        }



    }

    @Nested
    class RazorpayOrderDetailsTests{
        @Nested
        class GetRazorpayOrderDetailsTests {
            @Test
            void shouldReturnRazorpayOrderDetailsSuccessfully() {

                // Given
                Long paymentId = 1L;
                String userId = "testUserId";

                PaymentAttempts attempt1 = PaymentAttempts.builder()
                        .id(1L)
                        .razorpayOrderId("razorpay_order_1")
                        .build();

                PaymentAttempts attempt2 = PaymentAttempts.builder()
                        .id(2L)
                        .razorpayOrderId("razorpay_order_2")
                        .build();

                Payments payments = Payments.builder()
                        .id(paymentId)
                        .userId(userId)
                        .totalAmount(20000L)
                        .currency("INR")
                        .paymentAttempts(new ArrayList<>(
                                java.util.List.of(attempt1, attempt2)
                        ))
                        .build();

                when(paymentsRepo.findById(paymentId))
                        .thenReturn(java.util.Optional.of(payments));

                // When
                RazorpayOrderDetails result =
                        paymentService.getRazorpayOrderDetails(paymentId, userId);

                // Then
                assertNotNull(result);
                assertEquals("razorpay_order_2", result.getOrderId());
                assertEquals(20000L, result.getAmount());
                assertEquals("INR", result.getCurrency());
                assertEquals("testApiKey", result.getApiKey());

                verify(paymentsRepo).findById(paymentId);
            }

            @Test
            void shouldThrowExceptionWhenPaymentNotFound() {

                // Given
                Long paymentId = 1L;

                when(paymentsRepo.findById(paymentId))
                        .thenReturn(java.util.Optional.empty());

                // When & Then
                assertThrows(
                        java.util.NoSuchElementException.class,
                        () -> paymentService.getRazorpayOrderDetails(
                                paymentId,
                                "testUserId"
                        )
                );

                verify(paymentsRepo).findById(paymentId);
            }

            @Test
            void shouldThrowExceptionWhenUserIsUnauthorized() {

                // Given
                Long paymentId = 1L;

                Payments payments = Payments.builder()
                        .id(paymentId)
                        .userId("actualUser")
                        .totalAmount(20000L)
                        .currency("INR")
                        .build();

                when(paymentsRepo.findById(paymentId))
                        .thenReturn(java.util.Optional.of(payments));

                // When & Then
                RuntimeException exception = assertThrows(
                        RuntimeException.class,
                        () -> paymentService.getRazorpayOrderDetails(
                                paymentId,
                                "wrongUser"
                        )
                );

                assertEquals("Unauthorized access", exception.getMessage());

                verify(paymentsRepo).findById(paymentId);
            }
    }


}

    @Nested
    class VerifyPaymentTests{
        @Test
        void shouldReturnFalseWhenSignatureIsInvalid() {

            PaymentVerificationRequest dto =
                    PaymentVerificationRequest.builder()
                            .orderId("testOrderId")
                            .paymentId("testPaymentId")
                            .signature("invalidSignature")
                            .build();

            try (MockedStatic<Utils> utilsMock =
                         mockStatic(Utils.class)) {

                utilsMock.when(() ->
                        Utils.verifyPaymentSignature(
                                any(JSONObject.class),
                                eq("testApiSecret123")
                        )
                ).thenReturn(false);

                boolean result =
                        paymentService.verifyRazorpayPayment(
                                dto,
                                "testUserId"
                        );

                assertFalse(result);

                verifyNoInteractions(paymentAttemptsRepo);
                verifyNoInteractions(streamBridge);
            }
        }

        @Test
        void shouldThrowExceptionWhenPaymentAttemptNotFound() {

            PaymentVerificationRequest dto =
                    PaymentVerificationRequest.builder()
                            .orderId("testOrderId")
                            .paymentId("testPaymentId")
                            .signature("testSignature")
                            .build();

            when(paymentAttemptsRepo.findByRazorpayOrderId("testOrderId"))
                    .thenReturn(java.util.Optional.empty());

            try (MockedStatic<Utils> utilsMock =
                         mockStatic(Utils.class)) {

                utilsMock.when(() ->
                        Utils.verifyPaymentSignature(
                                any(JSONObject.class),
                                eq("testApiSecret123")
                        )
                ).thenReturn(true);

                assertThrows(
                        java.util.NoSuchElementException.class,
                        () -> paymentService.verifyRazorpayPayment(
                                dto,
                                "testUserId"
                        )
                );
            }
        }

        @Test
        void shouldReturnFalseWhenUserIsUnauthorized() {

            PaymentVerificationRequest dto =
                    PaymentVerificationRequest.builder()
                            .orderId("testOrderId")
                            .paymentId("testPaymentId")
                            .signature("testSignature")
                            .build();

            Payments payments = Payments.builder()
                    .id(1L)
                    .orderId(1234L)
                    .userId("actualUser")
                    .build();

            PaymentAttempts attempt = PaymentAttempts.builder()
                    .id(1L)
                    .razorpayOrderId("testOrderId")
                    .payment(payments)
                    .status(AttemptStatus.CREATED)
                    .build();

            when(paymentAttemptsRepo.findByRazorpayOrderId("testOrderId"))
                    .thenReturn(java.util.Optional.of(attempt));

            try (MockedStatic<Utils> utilsMock =
                         mockStatic(Utils.class)) {

                utilsMock.when(() ->
                        Utils.verifyPaymentSignature(
                                any(JSONObject.class),
                                eq("testApiSecret123")
                        )
                ).thenReturn(true);

                boolean result =
                        paymentService.verifyRazorpayPayment(
                                dto,
                                "wrongUser"
                        );

                assertFalse(result);

                verifyNoInteractions(razorpayClient);
                verifyNoInteractions(streamBridge);
            }
        }
        @Test
        void shouldReturnTrueIfPaymentAlreadySuccessful() {

            PaymentVerificationRequest dto =
                    PaymentVerificationRequest.builder()
                            .orderId("testOrderId")
                            .paymentId("testPaymentId")
                            .signature("testSignature")
                            .build();

            Payments payments = Payments.builder()
                    .id(1L)
                    .orderId(1234L)
                    .userId("testUserId")
                    .build();

            PaymentAttempts attempt = PaymentAttempts.builder()
                    .id(1L)
                    .razorpayOrderId("testOrderId")
                    .payment(payments)
                    .status(AttemptStatus.SUCCESS)
                    .build();

            when(paymentAttemptsRepo.findByRazorpayOrderId("testOrderId"))
                    .thenReturn(java.util.Optional.of(attempt));

            try (MockedStatic<Utils> utilsMock =
                         mockStatic(Utils.class)) {

                utilsMock.when(() ->
                        Utils.verifyPaymentSignature(
                                any(JSONObject.class),
                                eq("testApiSecret123")
                        )
                ).thenReturn(true);

                boolean result =
                        paymentService.verifyRazorpayPayment(
                                dto,
                                "testUserId"
                        );

                assertTrue(result);

                verifyNoInteractions(razorpayClient);
                verifyNoInteractions(streamBridge);
            }
        }

        @Test
        void shouldReturnFalseWhenRazorpayPaymentIsNotCaptured() throws RazorpayException {

            PaymentVerificationRequest dto =
                    PaymentVerificationRequest.builder()
                            .orderId("testOrderId")
                            .paymentId("testPaymentId")
                            .signature("testSignature")
                            .build();

            Payments payments = Payments.builder()
                    .id(1L)
                    .orderId(1234L)
                    .userId("testUserId")
                    .totalAmount(20000L)
                    .build();

            PaymentAttempts attempt = PaymentAttempts.builder()
                    .razorpayOrderId("testOrderId")
                    .payment(payments)
                    .status(AttemptStatus.CREATED)
                    .build();

            Payment razorpayPayment = mock(Payment.class);

            when(paymentAttemptsRepo.findByRazorpayOrderId("testOrderId"))
                    .thenReturn(java.util.Optional.of(attempt));

            when(razorpayClient.payments.fetch("testPaymentId"))
                    .thenReturn(razorpayPayment);

            when(razorpayPayment.get("status"))
                    .thenReturn("failed");

            try (MockedStatic<Utils> utilsMock =
                         mockStatic(Utils.class)) {

                utilsMock.when(() ->
                        Utils.verifyPaymentSignature(
                                any(JSONObject.class),
                                eq("testApiSecret123")
                        )
                ).thenReturn(true);

                boolean result =
                        paymentService.verifyRazorpayPayment(
                                dto,
                                "testUserId"
                        );

                assertFalse(result);

                verify(razorpayClient.payments)
                        .fetch("testPaymentId");

                verifyNoInteractions(streamBridge);
            }
        }

        @Test
        void shouldReturnFalseWhenAmountDoesNotMatch() throws RazorpayException {

            PaymentVerificationRequest dto =
                    PaymentVerificationRequest.builder()
                            .orderId("testOrderId")
                            .paymentId("testPaymentId")
                            .signature("testSignature")
                            .build();

            Payments payments = Payments.builder()
                    .id(1L)
                    .orderId(1234L)
                    .userId("testUserId")
                    .totalAmount(20000L)
                    .build();

            PaymentAttempts attempt = PaymentAttempts.builder()
                    .razorpayOrderId("testOrderId")
                    .payment(payments)
                    .status(AttemptStatus.CREATED)
                    .build();

            Payment razorpayPayment = mock(Payment.class);

            when(paymentAttemptsRepo.findByRazorpayOrderId("testOrderId"))
                    .thenReturn(java.util.Optional.of(attempt));

            when(razorpayClient.payments.fetch("testPaymentId"))
                    .thenReturn(razorpayPayment);

            when(razorpayPayment.get("status"))
                    .thenReturn("captured");

            when(razorpayPayment.get("amount"))
                    .thenReturn(15000L);

            try (MockedStatic<Utils> utilsMock =
                         mockStatic(Utils.class)) {

                utilsMock.when(() ->
                        Utils.verifyPaymentSignature(
                                any(JSONObject.class),
                                eq("testApiSecret123")
                        )
                ).thenReturn(true);

                boolean result =
                        paymentService.verifyRazorpayPayment(
                                dto,
                                "testUserId"
                        );

                assertFalse(result);

                assertEquals(
                        AttemptStatus.CREATED,
                        attempt.getStatus()
                );

                verifyNoInteractions(streamBridge);
            }
        }

        @Test
        void shouldVerifyPaymentSuccessfully() throws RazorpayException {

            PaymentVerificationRequest dto =
                    PaymentVerificationRequest.builder()
                            .orderId("testOrderId")
                            .paymentId("testPaymentId")
                            .signature("testSignature")
                            .build();

            Payments payments = Payments.builder()
                    .id(1L)
                    .orderId(1234L)
                    .userId("testUserId")
                    .totalAmount(20000L)
                    .currency("INR")
                    .status(PaymentStatus.CREATED)
                    .build();

            PaymentAttempts attempt = PaymentAttempts.builder()
                    .id(1L)
                    .razorpayOrderId("testOrderId")
                    .payment(payments)
                    .status(AttemptStatus.CREATED)
                    .build();

            Payment razorpayPayment = mock(Payment.class);

            when(paymentAttemptsRepo.findByRazorpayOrderId("testOrderId"))
                    .thenReturn(java.util.Optional.of(attempt));

            when(razorpayClient.payments.fetch("testPaymentId"))
                    .thenReturn(razorpayPayment);

            when(razorpayPayment.get("status"))
                    .thenReturn("captured");

            when(razorpayPayment.get("amount"))
                    .thenReturn(20000L);

            try (MockedStatic<Utils> utilsMock =
                         mockStatic(Utils.class)) {

                utilsMock.when(() ->
                        Utils.verifyPaymentSignature(
                                any(JSONObject.class),
                                eq("testApiSecret123")
                        )
                ).thenReturn(true);

                boolean result =
                        paymentService.verifyRazorpayPayment(
                                dto,
                                "testUserId"
                        );

                assertTrue(result);

                assertEquals(
                        AttemptStatus.SUCCESS,
                        attempt.getStatus()
                );

                assertEquals(
                        "testPaymentId",
                        attempt.getRazorpayPaymentId()
                );

                assertEquals(
                        PaymentStatus.SUCCESS,
                        payments.getStatus()
                );

                verify(streamBridge).send(
                        eq("paymentSuccess-out-0"),
                        any(PaymentSuccessEvent.class)
                );
            }
        }

    }


}