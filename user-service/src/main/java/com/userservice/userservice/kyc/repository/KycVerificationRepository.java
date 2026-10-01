package com.userservice.userservice.kyc.repository;

import com.userservice.userservice.kyc.entity.KycVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KycVerificationRepository extends JpaRepository<KycVerification, Long> {

    Optional<KycVerification> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    List<KycVerification> findByUserIdOrderByCreatedAtDesc(Long userId);
}
