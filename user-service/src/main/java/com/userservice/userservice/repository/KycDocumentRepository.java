package com.userservice.userservice.repository;

import com.userservice.userservice.entity.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {

    Optional<KycDocument> findFirstByUserIdOrderBySubmittedAtDesc(Long userId);

    List<KycDocument> findByUserIdOrderBySubmittedAtDesc(Long userId);
}
