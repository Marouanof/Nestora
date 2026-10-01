package com.userservice.userservice.kyc.service;

import com.userservice.userservice.dto.KycStatusResponse;
import com.userservice.userservice.exception.InvalidActionException;
import com.userservice.userservice.exception.KycNotFoundException;
import com.userservice.userservice.exception.UserNotFoundException;
import com.userservice.userservice.kyc.client.AiKycClient;
import com.userservice.userservice.kyc.client.AiKycResult;
import com.userservice.userservice.kyc.entity.KycAttempt;
import com.userservice.userservice.kyc.entity.KycDocument;
import com.userservice.userservice.kyc.entity.KycVerification;
import com.userservice.userservice.kyc.enums.KycDocumentType;
import com.userservice.userservice.kyc.enums.KycProviderType;
import com.userservice.userservice.kyc.enums.KycVerificationStatus;
import com.userservice.userservice.kyc.provider.KycProvider;
import com.userservice.userservice.kyc.provider.ProviderResponse;
import com.userservice.userservice.kyc.repository.KycAttemptRepository;
import com.userservice.userservice.kyc.repository.KycDocumentRepository;
import com.userservice.userservice.kyc.repository.KycVerificationRepository;
import com.userservice.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Workflow de vérification d'identité. Domaine isolé dans le package {@code kyc.*}
 * afin d'être extrait vers un futur KYC-Service sans dépendre du User-Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KycService {

    private final KycVerificationRepository verificationRepository;
    private final KycDocumentRepository documentRepository;
    private final KycAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final List<KycProvider> providers;
    private final AiKycClient aiKycClient;

    public Optional<KycVerification> getLatestVerification(Long userId) {
        return verificationRepository.findFirstByUserIdOrderByCreatedAtDesc(userId);
    }

    public Optional<KycDocument> getLatestDocument(Long userId) {
        return getLatestVerification(userId)
                .flatMap(verification -> documentRepository
                        .findFirstByVerificationIdOrderByUploadedAtDesc(verification.getId()));
    }

    // RGPD : suppression de toutes les données KYC d'un utilisateur
    @Transactional
    public void deleteAllForUser(Long userId) {
        List<KycVerification> verifications = verificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (KycVerification verification : verifications) {
            attemptRepository.deleteAll(
                    attemptRepository.findByVerificationIdOrderByAttemptedAtAsc(verification.getId()));
            documentRepository.deleteAll(
                    documentRepository.findByVerificationIdOrderByUploadedAtDesc(verification.getId()));
            verificationRepository.delete(verification);
        }
        log.info("Deleted {} KYC verifications for user {}", verifications.size(), userId);
    }

    @Transactional
    public KycVerification startVerification(Long userId) {
        KycVerification verification = getOrCreateVerification(userId);
        recordAttempt(verification, verification.getStatus());
        return verification;
    }

    @Transactional
    public KycDocument submitFile(Long userId, String fileUrl, String side) {
        KycVerification verification = getOrCreateVerification(userId);
        KycDocument doc = getOrCreateDocument(verification);

        switch (side.toLowerCase()) {
            case "kyc_recto" -> doc.setRectoUrl(fileUrl);
            case "kyc_verso" -> doc.setVersoUrl(fileUrl);
            case "kyc_selfie" -> doc.setSelfieUrl(fileUrl);
            default -> throw new IllegalArgumentException("Unknown KYC file side: " + side);
        }

        boolean complete = isNotBlank(doc.getRectoUrl()) && isNotBlank(doc.getVersoUrl())
                && isNotBlank(doc.getSelfieUrl());
        if (complete) {
            doc.setStatus(KycVerificationStatus.PENDING);
            doc.setRejectionReason(null);
            doc.setVerifiedAt(null);
            doc.setUploadedAt(LocalDateTime.now());

            verification.setStatus(KycVerificationStatus.PENDING);
            verification.setRejectionReason(null);
            verification.setVerifiedAt(null);
            verification.setSubmittedAt(LocalDateTime.now());
        }

        doc = documentRepository.save(doc);
        verificationRepository.save(verification);

        if (complete) {
            recordAttempt(verification, KycVerificationStatus.PENDING);
            if (aiKycClient.isEnabled()) {
                AiKycResult result = aiKycClient.analyze(doc);
                log.info("AI analysis for verification {}: reviewed={}, confidence={}, flags={}",
                        verification.getId(), result.reviewed(), result.confidence(), result.flags());
            }
        }

        return doc;
    }

    // ✅ Type de pièce d'identité (CIN, passeport, permis)
    @Transactional
    public KycDocument setDocumentType(Long userId, KycDocumentType documentType) {
        KycVerification verification = getOrCreateVerification(userId);
        KycDocument doc = getOrCreateDocument(verification);
        doc.setDocumentType(documentType);
        return documentRepository.save(doc);
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private KycDocument getOrCreateDocument(KycVerification verification) {
        return documentRepository
                .findFirstByVerificationIdOrderByUploadedAtDesc(verification.getId())
                .filter(d -> d.getStatus() != KycVerificationStatus.VERIFIED)
                .orElseGet(() -> KycDocument.builder()
                        .verification(verification)
                        .status(KycVerificationStatus.PENDING)
                        .build());
    }

    public static final int MAX_KYC_ATTEMPTS = 5;

    @Transactional(readOnly = true)
    public KycStatusResponse getStatusDto(Long userId) {
        KycVerification verification = verificationRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElse(null);
        if (verification == null) {
            return new KycStatusResponse(
                    KycVerificationStatus.NOT_STARTED.name(), 0, MAX_KYC_ATTEMPTS, null, true, null);
        }
        long attempts = attemptRepository.countByVerificationId(verification.getId());
        boolean canRetry = verification.getStatus() == KycVerificationStatus.REJECTED
                && attempts < MAX_KYC_ATTEMPTS;
        // Premier envoi aussi autorisé quand jamais soumis
        if (verification.getStatus() == KycVerificationStatus.NOT_STARTED) {
            canRetry = true;
        }
        return new KycStatusResponse(
                verification.getStatus().name(),
                (int) attempts,
                MAX_KYC_ATTEMPTS,
                verification.getRejectionReason(),
                canRetry,
                verification.getUpdatedAt());
    }

    @Transactional
    public KycVerification retry(Long userId) {
        KycVerification latest = verificationRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElse(null);
        if (latest != null && latest.getStatus() == KycVerificationStatus.VERIFIED) {
            throw new InvalidActionException("KYC déjà vérifié");
        }
        if (latest != null && (latest.getStatus() == KycVerificationStatus.PENDING
                || latest.getStatus() == KycVerificationStatus.IN_REVIEW)) {
            throw new InvalidActionException("Vérification déjà en cours");
        }
        if (latest != null && latest.getStatus() == KycVerificationStatus.REJECTED) {
            long attempts = attemptRepository.countByVerificationId(latest.getId());
            if (attempts >= MAX_KYC_ATTEMPTS) {
                throw new InvalidActionException("Nombre maximum de tentatives atteint");
            }
        }
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found");
        }
        KycVerification verification = KycVerification.builder()
                .userId(userId)
                .status(KycVerificationStatus.PENDING)
                .provider(KycProviderType.MANUAL)
                .startedAt(LocalDateTime.now())
                .build();
        KycProvider provider = providerFor(verification.getProvider());
        ProviderResponse providerResponse = provider.startVerification(userId);
        verification.setProviderReference(providerResponse.providerReference());
        verification = verificationRepository.save(verification);
        recordAttempt(verification, KycVerificationStatus.PENDING);
        return verification;
    }

    @Transactional
    public KycVerification approve(Long userId) {
        KycVerification verification = requireLatestVerification(userId);
        verification.setStatus(KycVerificationStatus.VERIFIED);
        verification.setVerifiedAt(LocalDateTime.now());
        verification.setRejectionReason(null);
        verificationRepository.save(verification);

        documentRepository.findFirstByVerificationIdOrderByUploadedAtDesc(verification.getId())
                .ifPresent(doc -> {
                    doc.setStatus(KycVerificationStatus.VERIFIED);
                    doc.setVerifiedAt(LocalDateTime.now());
                    doc.setRejectionReason(null);
                    documentRepository.save(doc);
                });
        return verification;
    }

    @Transactional
    public KycVerification reject(Long userId, String reason) {
        KycVerification verification = requireLatestVerification(userId);
        verification.setStatus(KycVerificationStatus.REJECTED);
        verification.setRejectedAt(LocalDateTime.now());
        verification.setRejectionReason(reason);
        verificationRepository.save(verification);

        documentRepository.findFirstByVerificationIdOrderByUploadedAtDesc(verification.getId())
                .ifPresent(doc -> {
                    doc.setStatus(KycVerificationStatus.REJECTED);
                    doc.setRejectionReason(reason);
                    documentRepository.save(doc);
                });
        return verification;
    }

    private KycVerification getOrCreateVerification(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found");
        }
        return verificationRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .filter(v -> v.getStatus() == KycVerificationStatus.NOT_STARTED
                        || v.getStatus() == KycVerificationStatus.PENDING
                        || v.getStatus() == KycVerificationStatus.IN_REVIEW)
                .orElseGet(() -> {
                    KycVerification verification = KycVerification.builder()
                            .userId(userId)
                            .status(KycVerificationStatus.PENDING)
                            .provider(KycProviderType.MANUAL)
                            .startedAt(LocalDateTime.now())
                            .build();
                    KycProvider provider = providerFor(verification.getProvider());
                    ProviderResponse providerResponse = provider.startVerification(userId);
                    verification.setProviderReference(providerResponse.providerReference());
                    return verificationRepository.save(verification);
                });
    }

    private KycVerification requireLatestVerification(Long userId) {
        return verificationRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new KycNotFoundException(
                        "Aucune soumission KYC trouvée pour l'utilisateur " + userId));
    }

    private void recordAttempt(KycVerification verification, KycVerificationStatus status) {
        long count = attemptRepository.countByVerificationId(verification.getId());
        KycAttempt attempt = KycAttempt.builder()
                .verification(verification)
                .attemptNumber((int) count + 1)
                .status(status)
                .attemptedAt(LocalDateTime.now())
                .build();
        attemptRepository.save(attempt);
    }

    private KycProvider providerFor(KycProviderType type) {
        return providers.stream()
                .filter(p -> p.getType() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No KycProvider registered for type " + type));
    }
}
