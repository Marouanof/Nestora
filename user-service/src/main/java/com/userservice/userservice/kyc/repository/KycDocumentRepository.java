package com.userservice.userservice.kyc.repository;

import com.userservice.userservice.kyc.entity.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {

    Optional<KycDocument> findFirstByVerificationIdOrderByUploadedAtDesc(Long verificationId);

    List<KycDocument> findByVerificationIdOrderByUploadedAtDesc(Long verificationId);
}
