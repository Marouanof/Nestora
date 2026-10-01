package com.userservice.userservice.dto;

public record UserReviewSummaryResponse(
        Long userId,
        double averageRating,
        long totalReviews
) {
}
