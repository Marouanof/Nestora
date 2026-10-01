package com.userservice.userservice.dto;

import java.time.LocalDateTime;

public record SessionResponse(
        String id,
        LocalDateTime createdAt,
        LocalDateTime lastUsedAt,
        String ip,
        String userAgent,
        boolean current,
        boolean revoked
) {
}
