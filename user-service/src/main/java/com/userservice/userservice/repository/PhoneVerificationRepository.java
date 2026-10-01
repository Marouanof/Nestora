package com.userservice.userservice.repository;

import com.userservice.userservice.entity.PhoneVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {

    Optional<PhoneVerification> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    List<PhoneVerification> findByUserIdAndCreatedAtAfter(Long userId, LocalDateTime since);

    void deleteByUserId(Long userId);
}
