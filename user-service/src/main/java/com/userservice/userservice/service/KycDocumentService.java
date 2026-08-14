package com.userservice.userservice.service;

import com.userservice.userservice.entity.KycDocument;
import com.userservice.userservice.entity.KycStatus;
import com.userservice.userservice.entity.User;
import com.userservice.userservice.exception.KycNotFoundException;
import com.userservice.userservice.exception.UserNotFoundException;
import com.userservice.userservice.repository.KycDocumentRepository;
import com.userservice.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycDocumentService {

    private final KycDocumentRepository kycDocumentRepository;
    private final UserRepository userRepository;

    public Optional<KycDocument> getLatestKyc(Long userId) {
        return kycDocumentRepository.findFirstByUserIdOrderBySubmittedAtDesc(userId);
    }

    @Transactional
    public KycDocument submitFile(Long userId, String fileUrl, String side) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        KycDocument doc = kycDocumentRepository.findFirstByUserIdOrderBySubmittedAtDesc(userId)
                .filter(d -> d.getStatus() != KycStatus.APPROVED)
                .orElse(null);

        if (doc == null) {
            doc = KycDocument.builder()
                    .user(user)
                    .status(KycStatus.PENDING)
                    .submittedAt(LocalDateTime.now())
                    .build();
        }

        if ("kyc_recto".equalsIgnoreCase(side)) {
            doc.setRectoUrl(fileUrl);
        } else {
            doc.setVersoUrl(fileUrl);
        }

        boolean complete = doc.getRectoUrl() != null && !doc.getRectoUrl().isBlank()
                && doc.getVersoUrl() != null && !doc.getVersoUrl().isBlank();
        if (complete) {
            doc.setStatus(KycStatus.PENDING);
            doc.setRejectionReason(null);
            doc.setVerifiedAt(null);
            doc.setSubmittedAt(LocalDateTime.now());
        }

        return kycDocumentRepository.save(doc);
    }

    @Transactional
    public KycDocument approve(Long userId) {
        KycDocument doc = requireLatest(userId);
        doc.setStatus(KycStatus.APPROVED);
        doc.setVerifiedAt(LocalDateTime.now());
        doc.setRejectionReason(null);
        return kycDocumentRepository.save(doc);
    }

    @Transactional
    public KycDocument reject(Long userId, String reason) {
        KycDocument doc = requireLatest(userId);
        doc.setStatus(KycStatus.REJECTED);
        doc.setVerifiedAt(null);
        doc.setRejectionReason(reason);
        return kycDocumentRepository.save(doc);
    }

    private KycDocument requireLatest(Long userId) {
        return kycDocumentRepository.findFirstByUserIdOrderBySubmittedAtDesc(userId)
                .orElseThrow(() -> new KycNotFoundException(
                        "Aucune soumission KYC trouvée pour l'utilisateur " + userId));
    }
}
