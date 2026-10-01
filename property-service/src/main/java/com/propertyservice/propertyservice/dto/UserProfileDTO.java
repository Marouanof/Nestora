package com.propertyservice.propertyservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
        if (Boolean.TRUE.equals(kycVerified)) {
            return true;
        }
        if (kycStatus == null) {
            return kycRectoUrl != null && !kycRectoUrl.trim().isEmpty()
                    && kycVersoUrl != null && !kycVersoUrl.trim().isEmpty();
        }
        // user-service émet "VERIFIED" (KycVerificationStatus) ; "APPROVED" gardé pour compat
        return "VERIFIED".equalsIgnoreCase(kycStatus) || "APPROVED".equalsIgnoreCase(kycStatus);
    }
}
