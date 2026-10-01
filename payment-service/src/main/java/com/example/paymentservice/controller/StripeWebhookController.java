package com.example.paymentservice.controller;

import com.example.paymentservice.service.PaymentService;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhook/payment")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final PaymentService paymentService;

    @Value("${app.stripe.webhook-secret:}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<String> handle(@RequestBody String payload,
                                         @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("Webhook recu sans STRIPE_WEBHOOK_SECRET configuree : ignore");
            return ResponseEntity.ok("ignored (no webhook secret)");
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (Exception e) {
            log.warn("Signature Stripe invalide: {}", e.getMessage());
            return ResponseEntity.badRequest().body("invalid signature");
        }

        try {
            switch (event.getType()) {
                case "checkout.session.completed" -> {
                    Session session = (Session) event.getDataObjectDeserializer()
                            .getObject().orElse(null);
                    if (session != null && "paid".equalsIgnoreCase(session.getPaymentStatus())) {
                        paymentService.handleSessionCompleted(session.getId(), session.getPaymentIntent());
                    }
                }
                case "checkout.session.expired", "checkout.session.async_payment_failed" ->
                        paymentService.handleSessionExpired(sessionIdOf(event));
                default -> log.debug("Event Stripe ignore: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Erreur traitement webhook: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("handler error");
        }
        return ResponseEntity.ok("ok");
    }

    private String sessionIdOf(Event event) {
        try {
            Object o = event.getDataObjectDeserializer().getObject().orElse(null);
            if (o instanceof Session s) return s.getId();
        } catch (Exception ignored) {}
        return null;
    }
}
