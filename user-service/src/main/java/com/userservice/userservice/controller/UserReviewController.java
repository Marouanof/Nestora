package com.userservice.userservice.controller;

import com.userservice.userservice.dto.UserReviewResponse;
import com.userservice.userservice.dto.UserReviewSummaryResponse;
import com.userservice.userservice.service.UserReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/{userId}/reviews")
@RequiredArgsConstructor
public class UserReviewController {

    private final UserReviewService userReviewService;

    // Public : voir les reviews reçues par un utilisateur
    @GetMapping
    public ResponseEntity<List<UserReviewResponse>> getReviews(@PathVariable Long userId) {
        return ResponseEntity.ok(userReviewService.getReviewsForUser(userId));
    }

    // Public : note moyenne d'un utilisateur
    @GetMapping("/summary")
    public ResponseEntity<UserReviewSummaryResponse> getSummary(@PathVariable Long userId) {
        return ResponseEntity.ok(userReviewService.getSummary(userId));
    }

    // Protégé : laisser une review sur un autre utilisateur
    @PostMapping
    public ResponseEntity<UserReviewResponse> createReview(
            @RequestHeader("X-Auth-User-Id") String reviewerIdHeader,
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body) {

        Long reviewerId = Long.valueOf(reviewerIdHeader);
        int rating = ((Number) body.getOrDefault("rating", 0)).intValue();
        String comment = (String) body.get("comment");
        Object bookingIdObj = body.get("bookingId");
        Long bookingId = bookingIdObj != null ? ((Number) bookingIdObj).longValue() : null;

        UserReviewResponse response = userReviewService.create(reviewerId, userId, rating, comment, bookingId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
