package com.userservice.userservice.service;

import com.userservice.userservice.dto.UserReviewResponse;
import com.userservice.userservice.dto.UserReviewSummaryResponse;
import com.userservice.userservice.entity.User;
import com.userservice.userservice.entity.UserReview;
import com.userservice.userservice.exception.InvalidActionException;
import com.userservice.userservice.exception.UserNotFoundException;
import com.userservice.userservice.repository.UserRepository;
import com.userservice.userservice.repository.UserReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserReviewService {

    private final UserReviewRepository userReviewRepository;
    private final UserRepository userRepository;

    @Transactional
    public UserReviewResponse create(Long reviewerId, Long revieweeId, int rating, String comment, Long bookingId) {
        if (reviewerId.equals(revieweeId)) {
            throw new InvalidActionException("Vous ne pouvez pas vous évaluer vous-même");
        }
        if (rating < 1 || rating > 5) {
            throw new InvalidActionException("La note doit être comprise entre 1 et 5");
        }
        if (bookingId != null && userReviewRepository.existsByBookingId(bookingId)) {
            throw new InvalidActionException("Une review existe déjà pour ce séjour");
        }

        User reviewee = userRepository.findById(revieweeId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur à évaluer introuvable"));
        if (!reviewee.isEnabled()) {
            throw new InvalidActionException("Cet utilisateur ne peut pas être évalué");
        }

        UserReview review = new UserReview(reviewerId, revieweeId, bookingId, rating, comment);
        return toResponse(userReviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public List<UserReviewResponse> getReviewsForUser(Long revieweeId) {
        return userReviewRepository.findByRevieweeIdOrderByCreatedAtDesc(revieweeId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserReviewSummaryResponse getSummary(Long userId) {
        List<UserReview> reviews = userReviewRepository.findByRevieweeIdOrderByCreatedAtDesc(userId);
        double average = reviews.stream().mapToInt(UserReview::getRating).average().orElse(0.0);
        // Arrondi à 1 décimale
        double rounded = Math.round(average * 10) / 10.0;
        return new UserReviewSummaryResponse(userId, rounded, reviews.size());
    }

    @Transactional
    public void deleteOwnReview(Long reviewId, Long requesterId) {
        UserReview review = userReviewRepository.findById(reviewId)
                .orElseThrow(() -> new InvalidActionException("Review introuvable"));
        if (!review.getReviewerId().equals(requesterId)) {
            throw new InvalidActionException("Vous ne pouvez supprimer que vos propres reviews");
        }
        userReviewRepository.delete(review);
    }

    private UserReviewResponse toResponse(UserReview review) {
        String reviewerName = userRepository.findById(review.getReviewerId())
                .map(u -> u.getFirstName() + " " + u.getLastName().charAt(0) + ".")
                .orElse("Utilisateur supprimé");

        return UserReviewResponse.builder()
                .id(review.getId())
                .reviewerId(review.getReviewerId())
                .reviewerName(reviewerName)
                .revieweeId(review.getRevieweeId())
                .bookingId(review.getBookingId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
