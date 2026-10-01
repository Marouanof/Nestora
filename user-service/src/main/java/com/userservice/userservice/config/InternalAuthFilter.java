package com.userservice.userservice.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

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
        String rolesHeader = req.getHeader("X-Auth-Roles");

        // Reviews : lecture publique, écriture protégée
        boolean publicReviewsGet = "GET".equals(method)
                && (path.matches("/api/users/\\d+/reviews") || path.matches("/api/users/\\d+/reviews/summary"));
        boolean publicUserGet = "GET".equals(method)
                && (path.matches("/api/users/\\d+") || path.matches("/api/users/\\d+/full"));

        // Appels service-à-service : secret gateway obligatoire, sans userId (ex: GET /internal/users/{id})
        if (path.startsWith("/internal/")) {
            if (!isGatewaySecretValid(req.getHeader("X-Gateway-Secret"))) {
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid gateway secret");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        if (path.startsWith("/api/auth/") ||
                path.startsWith("/files/") ||
                path.equals("/actuator/health") ||
                path.equals("/swagger-ui.html") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                publicUserGet ||
                publicReviewsGet) { // /api/users/batch et POST /api/users/\d+ exigent désormais le secret
            chain.doFilter(request, response);
            return;
        }

        // Batch inter-service : secret obligatoire (anti-scraping)
        if (path.equals("/api/users/batch")) {
            if (!isGatewaySecretValid(req.getHeader("X-Gateway-Secret"))) {
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid gateway secret");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        // Routes protégées : la requête doit venir de la gateway (secret partagé).
        // Sans ce contrôle, quiconque accède directement au service peut forger X-Auth-User-Id.
        if (!isGatewaySecretValid(req.getHeader("X-Gateway-Secret"))) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid gateway secret");
            return;
        }

        String userId = req.getHeader("X-Auth-User-Id");
        if (userId == null || userId.isBlank()) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing X-Auth-User-Id");
            return;
        }
        if (path.startsWith("/api/admin/")) {
            if (rolesHeader == null || !rolesHeader.contains("ROLE_ADMIN")) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin role required");
                return;
            }
        }
        try {
            Long.valueOf(userId);
        } catch (NumberFormatException e) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid user id");
            return;
        }
        chain.doFilter(request, response);
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
