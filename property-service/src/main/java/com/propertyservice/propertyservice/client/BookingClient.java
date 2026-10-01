package com.propertyservice.propertyservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingClient {

    private final RestTemplate restTemplate;

    @Value("${app.services.booking-service-url:http://localhost:8083}")
    private String bookingServiceUrl;

    @Value("${app.gateway.shared-secret}")
    private String gatewaySharedSecret;

    /**
     * Vérifie auprès du booking-service que l'utilisateur a terminé un séjour
     * dans cette propriété (statut COMPLETED). Fail-closed : en cas d'erreur
     * de communication, on considère le séjour non vérifié.
     */
    public boolean hasCompletedStay(Long propertyId, Long userId) {
        try {
            String url = bookingServiceUrl + "/internal/bookings/completed/exists"
                    + "?propertyId=" + propertyId + "&userId=" + userId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Gateway-Secret", gatewaySharedSecret);

            Boolean result = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<Void>(headers), Boolean.class).getBody();

            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to verify completed stay for user {} on property {}: {}",
                    userId, propertyId, e.getMessage());
            return false;
        }
    }
}
