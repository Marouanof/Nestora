package com.example.paymentservice.service;

import com.example.paymentservice.config.RabbitConfig;
import com.example.paymentservice.dto.InitPaymentResponse;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.enu.PaymentStatus;
import com.example.paymentservice.messaging.BookingPaymentEvent;
import com.example.paymentservice.repository.PaymentRepository;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${app.stripe.stub:false}")
    private boolean stubEnabled;

    @Value("${app.stripe.success-url:http://localhost:5173/bookings/success?session_id={CHECKOUT_SESSION_ID}}")
    private String successUrl;

    @Value("${app.stripe.cancel-url:http://localhost:5173/bookings/cancel}")
    private String cancelUrl;

    @Value("${app.stripe.currency:mad}")
    private String currency;

    @Value("${app.services.booking-service-url:http://localhost:8083}")
    private String bookingServiceUrl;

    @Value("${app.gateway.shared-secret:change-me}")
    private String gatewaySharedSecret;

    public InitPaymentResponse initPayment(Long bookingId, Long tenantId) {
        Map<String, Object> booking = fetchBooking(bookingId);
        BigDecimal total = new BigDecimal(String.valueOf(booking.get("totalPrice")));

        // Mode stub (local/test, Maroc : Stripe indisponible) : pas d'appel Stripe,
        // session fictive qui alimente la même chaîne (webhook simulé -> PAYMENT_SUCCESS).
        if (stubEnabled) {
            return initStubPayment(bookingId, tenantId, total);
        }

        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new IllegalStateException("STRIPE_SECRET_KEY non configuree");
        }

        long amountMinor = total.multiply(BigDecimal.valueOf(100)).longValueExact();

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .setClientReferenceId(String.valueOf(bookingId))
                    .putMetadata("bookingId", String.valueOf(bookingId))
                    .putMetadata("tenantId", String.valueOf(tenantId))
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency(currency)
                                    .setUnitAmount(amountMinor)
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("Reservation #" + bookingId)
                                            .build())
                                    .build())
                            .build())
                    .build();

            Session session = Session.create(params);

            Payment payment = paymentRepository.save(Payment.builder()
                    .bookingId(bookingId)
                    .tenantId(tenantId)
                    .amount(total)
                    .currency(currency)
                    .status(PaymentStatus.PROCESSING)
                    .stripeSessionId(session.getId())
                    .checkoutUrl(session.getUrl())
                    .build());

            publish(bookingId, false, "PAYMENT_INITIATED", RabbitConfig.PAYMENT_INITIATED_ROUTING_KEY, true);
            log.info("Checkout cree: booking={} session={} amount={} {}", bookingId, session.getId(), total, currency);

            return InitPaymentResponse.builder()
                    .paymentId(payment.getId())
                    .bookingId(bookingId)
                    .amount(total)
                    .currency(currency)
                    .checkoutUrl(session.getUrl())
                    .sessionId(session.getId())
                    .status(PaymentStatus.PROCESSING.name())
                    .build();
        } catch (Exception e) {
            log.error("Echec creation Checkout booking {}: {}", bookingId, e.getMessage());
            throw new RuntimeException("Stripe Checkout failed: " + e.getMessage(), e);
        }
    }

    private InitPaymentResponse initStubPayment(Long bookingId, Long tenantId, BigDecimal total) {
        String sessionId = "stub_" + UUID.randomUUID();
        String checkoutUrl = successUrl.replace("{CHECKOUT_SESSION_ID}", sessionId);

        Payment payment = paymentRepository.save(Payment.builder()
                .bookingId(bookingId)
                .tenantId(tenantId)
                .amount(total)
                .currency(currency)
                .status(PaymentStatus.PROCESSING)
                .stripeSessionId(sessionId)
                .checkoutUrl(checkoutUrl)
                .build());

        publish(bookingId, false, "PAYMENT_INITIATED", RabbitConfig.PAYMENT_INITIATED_ROUTING_KEY, true);
        log.info("Checkout STUB cree: booking={} session={} amount={} {}", bookingId, sessionId, total, currency);

        return InitPaymentResponse.builder()
                .paymentId(payment.getId())
                .bookingId(bookingId)
                .amount(total)
                .currency(currency)
                .checkoutUrl(checkoutUrl)
                .sessionId(sessionId)
                .status(PaymentStatus.PROCESSING.name())
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchBooking(Long bookingId) {
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("X-Gateway-Secret", gatewaySharedSecret);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> res = restTemplate.exchange(
                    bookingServiceUrl + "/internal/bookings/" + bookingId,
                    HttpMethod.GET, entity, Map.class);
            if (res.getBody() == null) throw new RuntimeException("Booking introuvable: " + bookingId);
            return res.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Booking-service inaccessible: " + e.getMessage(), e);
        }
    }

    public void handleSessionCompleted(String sessionId, String paymentIntentId) {
        paymentRepository.findByStripeSessionId(sessionId).ifPresent(p -> {
            p.setStatus(PaymentStatus.SUCCEEDED);
            p.setStripePaymentIntentId(paymentIntentId);
            paymentRepository.save(p);
            publish(p.getBookingId(), true, "PAYMENT_SUCCESS", RabbitConfig.PAYMENT_SUCCESS_ROUTING_KEY, false);
            log.info("Paiement SUCCEEDED booking={} session={}", p.getBookingId(), sessionId);
        });
    }

    public void handleSessionExpired(String sessionId) {
        paymentRepository.findByStripeSessionId(sessionId).ifPresent(p -> {
            p.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(p);
            publish(p.getBookingId(), false, "PAYMENT_FAILED", RabbitConfig.PAYMENT_FAILED_ROUTING_KEY, false);
            log.info("Paiement FAILED booking={} session={}", p.getBookingId(), sessionId);
        });
    }

    private void publish(Long bookingId, boolean success, String reason, String routingKey, boolean silent) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.BOOKING_EXCHANGE, routingKey,
                    BookingPaymentEvent.builder().bookingId(bookingId).success(success).reason(reason).build());
        } catch (Exception e) {
            if (!silent) throw new RuntimeException(e);
            log.warn("Publish {} ignore (exchange pas encore pret): {}", routingKey, e.getMessage());
        }
    }
}
