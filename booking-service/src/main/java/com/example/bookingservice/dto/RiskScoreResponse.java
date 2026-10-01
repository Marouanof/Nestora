package com.example.bookingservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskScoreResponse {
    @com.fasterxml.jackson.annotation.JsonAlias({"userId", "user_id"})
    private Long userId;
    private int score;
    @com.fasterxml.jackson.annotation.JsonAlias("riskLevel")
    private String risk_level;

    public String getRisk_level() {
        return risk_level != null ? risk_level.toUpperCase() : null;
    }
}
