package com.userservice.userservice.kyc.provider;

import com.userservice.userservice.kyc.enums.KycProviderType;

/**
 * Abstraction des providers de vérification d'identité externes
 * (Stripe Identity, Persona, Sumsub, Veriff, ... à venir).
 * La vérification manuelle (comportement actuel) est assurée par
 * {@link ManualKycProvider}.
 */
public interface KycProvider {

    KycProviderType getType();

    /**
     * Démarre une session de vérification auprès du provider externe.
     */
    ProviderResponse startVerification(Long userId);

    /**
     * Soumet les URLs des pièces (recto/verso) au provider externe.
     */
    ProviderResponse submitDocument(String providerReference, String rectoUrl, String versoUrl);

    /**
     * Interroge le statut d'une vérification auprès du provider externe.
     */
    ProviderResponse checkStatus(String providerReference);
}
