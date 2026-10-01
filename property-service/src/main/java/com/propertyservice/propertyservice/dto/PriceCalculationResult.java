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
}
