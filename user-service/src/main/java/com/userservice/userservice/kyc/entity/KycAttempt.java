package com.userservice.userservice.kyc.entity;

import com.userservice.userservice.kyc.enums.KycVerificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verification_id", nullable = false)
    private KycVerification verification;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private KycVerificationStatus status;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;
}
