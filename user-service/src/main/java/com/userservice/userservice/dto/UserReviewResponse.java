package com.userservice.userservice.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UserReviewResponse(
        Long id,
        Long reviewerId,
        String reviewerName,
        Long revieweeId,
        Long bookingId,
        int rating,
        String comment,
        LocalDateTime createdAt
) {
}
