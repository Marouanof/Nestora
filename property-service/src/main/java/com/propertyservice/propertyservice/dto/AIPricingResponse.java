package com.propertyservice.propertyservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIPricingResponse {
    private Long property_id;
    @com.fasterxml.jackson.annotation.JsonAlias("suggested_price_eth")
    private BigDecimal suggested_price_mad;
    private String yield_improvement;
    private String note;

    public BigDecimal getSuggested_price_eth() {
        return suggested_price_mad;
    }

    public void setSuggested_price_eth(BigDecimal v) {
        this.suggested_price_mad = v;
    }
}
