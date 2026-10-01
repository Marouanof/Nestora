package com.userservice.userservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = "booking_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Column(name = "reviewee_id", nullable = false)
    private Long revieweeId;

    // Optionnel : lien avec le séjour (une seule review par booking)
    @Column(name = "booking_id")
    private Long bookingId;

    @Column(nullable = false)
    private int rating; // 1 à 5

    @Column(length = 1000)
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UserReview(Long reviewerId, Long revieweeId, Long bookingId, int rating, String comment) {
        this.reviewerId = reviewerId;
        this.revieweeId = revieweeId;
        this.bookingId = bookingId;
        this.rating = rating;
        this.comment = comment;
    }
}
