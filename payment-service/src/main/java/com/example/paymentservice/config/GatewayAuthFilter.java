package com.example.paymentservice.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * /api/payments/** exige secret gateway + X-Auth-User-Id.
 * /api/webhook/payment est public (sécurisé par signature Stripe).
 */
@Component
@Order(1)
public class GatewayAuthFilter implements Filter {

    @Value("${app.gateway.shared-secret:change-me}")
    private String gatewaySharedSecret;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getRequestURI();

        if (path.startsWith("/api/webhook/") || path.equals("/actuator/health")
                || path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
            chain.doFilter(request, response);
            return;
        }

        if (!MessageDigest.isEqual(
                String.valueOf(req.getHeader("X-Gateway-Secret")).getBytes(StandardCharsets.UTF_8),
                String.valueOf(gatewaySharedSecret).getBytes(StandardCharsets.UTF_8))) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid gateway secret");
            return;
        }
        String userId = req.getHeader("X-Auth-User-Id");
        if (userId == null || userId.isBlank()) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing X-Auth-User-Id");
            return;
        }
        try {
            Long.valueOf(userId);
        } catch (NumberFormatException e) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid user id");
            return;
        }
        chain.doFilter(request, response);
    }
}
