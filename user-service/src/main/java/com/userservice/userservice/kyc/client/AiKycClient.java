package com.userservice.userservice.kyc.client;

import com.userservice.userservice.kyc.entity.KycDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client vers l'IA-Service pour l'analyse des pièces KYC
 * (OCR, qualité, comparaison faciale, détection de fraude).
 *
 * Désactivé par défaut ({@code app.kyc.ai-enabled=false}) : l'IA-Service n'expose
 * pas encore ces endpoints. Lorsqu'activé, toute indisponibilité de l'IA est
 * tolérée et ne bloque jamais le workflow.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiKycClient {

    private final RestTemplate restTemplate;

    @Value("${app.kyc.ai-url:http://localhost:8000}")
    private String aiServiceUrl;

    @Value("${app.kyc.ai-enabled:false}")
    private boolean aiEnabled;

    public boolean isEnabled() {
        return aiEnabled;
    }

    public AiKycResult analyze(KycDocument document) {
        if (!aiEnabled) {
            return AiKycResult.notRun();
        }

        try {
            String url = aiServiceUrl + "/kyc/analyze";
            Map<String, Object> payload = Map.of(
                    "rectoUrl", document.getRectoUrl(),
                    "versoUrl", document.getVersoUrl(),
                    "documentType", document.getDocumentType() != null ? document.getDocumentType().name() : null,
                    "country", document.getCountry()
            );
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload);
            ResponseEntity<AiKycResult> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, AiKycResult.class);
            AiKycResult result = response.getBody();
            return result != null ? result : AiKycResult.notRun();
        } catch (Exception e) {
            log.warn("AI-Service KYC analysis unavailable for document {}: {}", document.getId(), e.getMessage());
            return AiKycResult.notRun();
        }
    }
}
