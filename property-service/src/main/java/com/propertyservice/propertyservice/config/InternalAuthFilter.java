package com.propertyservice.propertyservice.config;

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
 * Toutes les requêtes non publiques doivent provenir de la gateway
 * (header X-Gateway-Secret ajouté après validation du JWT).
 * Sans ce contrôle, quiconque accède directement au service peut forger X-Auth-User-Id.
 */
@Component
@Order(1)
public class InternalAuthFilter implements Filter {

    @Value("${app.gateway.shared-secret}")
    private String gatewaySharedSecret;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getRequestURI();
        String method = req.getMethod();

        // Appels service-à-service : secret gateway obligatoire, sans userId
        // (ex: GET /internal/bookings/completed/exists, GET /internal/properties/{id})
        if (path.startsWith("/internal/")) {
            if (!isGatewaySecretValid(req.getHeader("X-Gateway-Secret"))) {
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid gateway secret");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        if (isPublic(path, method)) {
            chain.doFilter(request, response);
            return;
        }

        if (!isGatewaySecretValid(req.getHeader("X-Gateway-Secret"))) {
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

    private boolean isPublic(String path, String method) {
        if (!"GET".equals(method)) {
            return false;
        }
        // Consultation publique des annonces et reviews
        return path.equals("/api/properties") ||
                path.matches("/api/properties/\\d+") ||
                path.equals("/api/properties/search") ||
                path.startsWith("/api/properties/search/") ||
                path.equals("/api/properties/recommendations") ||
                path.matches("/api/properties/\\d+/booking-info") ||
                path.matches("/api/properties/\\d+/reviews.*") ||
                path.matches("/api/analytics/.*") ||
                path.startsWith("/files/") ||
                path.equals("/actuator/health") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs");
    }

    private boolean isGatewaySecretValid(String provided) {
        if (provided == null || gatewaySharedSecret == null) {
            return false;
        }
        // Comparaison en temps constant pour éviter les timing attacks
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                gatewaySharedSecret.getBytes(StandardCharsets.UTF_8));
    }
}
