package com.example.paymentservice.controller;

import com.example.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Simulation de webhook Stripe pour les environnements locaux/test où Stripe
 * est indisponible (ex. Maroc : pas d'onboarding, pas de MAD).
 * Actif UNIQUEMENT si app.stripe.stub=true (STRIPE_STUB=true), sinon 403.
 * Protégé comme le reste de l'API par X-Gateway-Secret + X-Auth-User-Id
 * (voir GatewayAuthFilter) : ne jamais activer en production.
 */
@RestController
@RequestMapping("/api/test/payments/stub")
@RequiredArgsConstructor
@Slf4j
public class StubPaymentController {

    private final PaymentService paymentService;

    @Value("${app.stripe.stub:false}")
    private boolean stubEnabled;

    @PostMapping("/complete")
    public ResponseEntity<?> complete(@RequestBody Map<String, Object> body) {
        if (!stubEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "stub desactive (STRIPE_STUB=true requis)"));
        }
        Object rawSession = body == null ? null : body.get("sessionId");
        if (rawSession == null || rawSession.toString().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "sessionId requis"));
        }
        String sessionId = rawSession.toString();
        boolean success = body.get("success") == null
                || Boolean.parseBoolean(body.get("success").toString());

        if (success) {
            paymentService.handleSessionCompleted(sessionId, "stub_pi_" + UUID.randomUUID().toString().replace("-", ""));
            log.info("STUB paiement SUCCEEDED session={}", sessionId);
        } else {
            paymentService.handleSessionExpired(sessionId);
            log.info("STUB paiement FAILED session={}", sessionId);
        }
        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "success", success,
                "status", success ? "SUCCEEDED" : "FAILED"));
    }
}
