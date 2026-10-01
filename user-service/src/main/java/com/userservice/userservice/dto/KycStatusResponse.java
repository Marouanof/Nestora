package com.userservice.userservice.dto;

import java.time.LocalDateTime;

public record KycStatusResponse(
        String status,
        int attempts,
        int maxAttempts,
        String rejectionReason,
        boolean canRetry,
        LocalDateTime updatedAt
) {
}
