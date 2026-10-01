package com.userservice.userservice.kyc.entity;

import com.userservice.userservice.kyc.enums.KycDocumentType;
import com.userservice.userservice.kyc.enums.KycVerificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_id")
    private KycVerification verification;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 30)
    private KycDocumentType documentType;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "recto_url", length = 500)
    private String rectoUrl;

    @Column(name = "verso_url", length = 500)
    private String versoUrl;

    @Column(name = "selfie_url", length = 500)
    private String selfieUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private KycVerificationStatus status = KycVerificationStatus.PENDING;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
}
