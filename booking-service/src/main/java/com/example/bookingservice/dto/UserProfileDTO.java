package com.example.bookingservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfileDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String photoUrl;
    private String kycRectoUrl;
    private String kycVersoUrl;
    private String kycStatus;
    private Boolean kycVerified;
    
    public boolean isKycComplete() {
        return isKycVerified() && hasPhoto();
    }

    /**
     * Identité vérifiée par l'admin (modèle Airbnb : seule une identité
     * vérifiée permet de réserver).
     */
    public boolean isKycVerified() {
        if (Boolean.TRUE.equals(kycVerified)) {
            return true;
        }
        // user-service émet "VERIFIED" (KycVerificationStatus) ; "APPROVED" gardé pour compat
        return "VERIFIED".equalsIgnoreCase(kycStatus) || "APPROVED".equalsIgnoreCase(kycStatus);
    }

    /** Dossier soumis, en attente de validation admin. */
    public boolean isKycPendingReview() {
        return "PENDING".equalsIgnoreCase(kycStatus) || "IN_REVIEW".equalsIgnoreCase(kycStatus);
    }

    /** Dossier refusé ou expiré : l'utilisateur doit soumettre à nouveau. */
    public boolean isKycRejected() {
        return "REJECTED".equalsIgnoreCase(kycStatus) || "EXPIRED".equalsIgnoreCase(kycStatus);
    }

    /** Des documents ont été téléversés (même si pas encore validés). */
    public boolean hasKycDocuments() {
        return kycRectoUrl != null && !kycRectoUrl.trim().isEmpty()
                && kycVersoUrl != null && !kycVersoUrl.trim().isEmpty();
    }

    private boolean hasPhoto() {
        return photoUrl != null && !photoUrl.trim().isEmpty();
    }
}
