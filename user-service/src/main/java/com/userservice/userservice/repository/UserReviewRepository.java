package com.userservice.userservice.repository;

import com.userservice.userservice.entity.UserReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserReviewRepository extends JpaRepository<UserReview, Long> {

    List<UserReview> findByRevieweeIdOrderByCreatedAtDesc(Long revieweeId);

    boolean existsByBookingId(Long bookingId);
}
