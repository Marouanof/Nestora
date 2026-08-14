// Nouveau fichier : PriceCalculationResult.java
package com.propertyservice.propertyservice.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PriceCalculationResult {
    private Long propertyId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer numberOfNights;
    private BigDecimal pricePerNight;
    private BigDecimal totalPrice;
    private BigDecimal securityDeposit;
    private Boolean isAvailable;
    private Integer minStayNights;

    // Méthode utilitaire pour l'affichage
    public String getSummary() {
        return String.format(
                "📍 Property #%d\n" +
                        "📅 %s to %s (%d nights)\n" +
                        "💰 %.4f EUR × %d nights = %.4f EUR\n" +
                        "🛡️ Security Deposit: %.4f EUR\n" +
                        "✅ Available: %s",
                propertyId, startDate, endDate, numberOfNights,
                pricePerNight, numberOfNights, totalPrice,
                securityDeposit,
                isAvailable ? "Yes" : "No"
        );
    }
}
