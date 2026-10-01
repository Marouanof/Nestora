package com.userservice.userservice.kyc.provider;

import com.userservice.userservice.kyc.enums.KycProviderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Provider par défaut : la vérification reste gérée manuellement
 * (upload des pièces puis décision admin), comme avant le refactor.
 * Sert de référence pour brancher un provider externe plus tard.
 */
@Slf4j
@Component
public class ManualKycProvider implements KycProvider {

    @Override
    public KycProviderType getType() {
        return KycProviderType.MANUAL;
    }

    @Override
    public ProviderResponse startVerification(Long userId) {
        log.debug("Manual KYC: starting verification for user {}", userId);
        return ProviderResponse.ok("manual-" + userId, "Vérification gérée manuellement");
    }

    @Override
    public ProviderResponse submitDocument(String providerReference, String rectoUrl, String versoUrl) {
        log.debug("Manual KYC: document stored locally, no external provider call");
        return ProviderResponse.ok(providerReference, "Pièces stockées, en attente de revue manuelle");
    }

    @Override
    public ProviderResponse checkStatus(String providerReference) {
        return ProviderResponse.ok(providerReference, "Statut géré localement");
    }
}
