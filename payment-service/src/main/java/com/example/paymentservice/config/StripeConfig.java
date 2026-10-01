package com.example.paymentservice.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class StripeConfig {
    @Value("${app.stripe.secret-key:}")
    private String secretKey;

    @PostConstruct
    public void init() {
        if (secretKey != null && !secretKey.isBlank()) {
            Stripe.apiKey = secretKey;
            log.info("Stripe initialised (key ****{})",
                    secretKey.substring(Math.max(0, secretKey.length() - 4)));
        } else {
            log.warn("STRIPE_SECRET_KEY vide : /api/payments/init retournera 503 tant que la cle n'est pas configuree. Webhook local : stripe listen --forward-to localhost:8084/api/webhook/payment");
        }
    }
}
