package com.userservice.userservice.kyc.repository;

import com.userservice.userservice.kyc.entity.KycAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KycAttemptRepository extends JpaRepository<KycAttempt, Long> {

    List<KycAttempt> findByVerificationIdOrderByAttemptedAtAsc(Long verificationId);

    long countByVerificationId(Long verificationId);
}
