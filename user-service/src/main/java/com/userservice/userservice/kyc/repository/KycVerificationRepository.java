package com.userservice.userservice.kyc.repository;

import com.userservice.userservice.kyc.entity.KycVerification;
import com.userservice.userservice.kyc.enums.KycVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KycVerificationRepository extends JpaRepository<KycVerification, Long> {

    Optional<KycVerification> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    List<KycVerification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * IDs des utilisateurs dont la DERNIÈRE vérification a l'un des statuts donnés.
     * Sert le filtre admin (ex : file des KYC PENDING à traiter).
     */
    @Query("SELECT DISTINCT v.userId FROM KycVerification v WHERE v.status IN :statuses "
            + "AND v.createdAt = (SELECT MAX(m.createdAt) FROM KycVerification m WHERE m.userId = v.userId)")
    List<Long> findUserIdsByLatestStatusIn(@Param("statuses") List<KycVerificationStatus> statuses);
}
