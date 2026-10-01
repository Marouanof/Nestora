package com.userservice.userservice.kyc.client;

import java.util.List;

/**
 * Résultat de l'analyse IA (OCR, qualité, face, fraude) d'un dossier KYC.
 * Le résultat IA est une information du workflow, jamais une autorité finale :
 * la décision reste côté KYC-Service / admin.
 */
public record AiKycResult(
        boolean reviewed,
        Integer confidence,
        List<String> flags,
        String message
) {
    public static AiKycResult notRun() {
        return new AiKycResult(false, null, List.of(), "Analyse IA non exécutée");
    }
}
