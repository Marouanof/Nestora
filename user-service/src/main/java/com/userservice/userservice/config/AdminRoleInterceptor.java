package com.userservice.userservice.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * La gateway valide le JWT et transmet les rôles via l'en-tête X-Auth-Roles.
 * Cet intercepteur applique le contrôle côté serveur pour toutes les routes /api/admin/**.
 */
@Component
public class AdminRoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String roles = request.getHeader("X-Auth-Roles");
        if (roles != null && roles.contains("ROLE_ADMIN")) {
            return true;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: Admin role required");
        return false;
    }
}
