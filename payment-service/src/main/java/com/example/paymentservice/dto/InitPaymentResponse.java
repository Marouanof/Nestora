package com.example.paymentservice.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class InitPaymentResponse {
    private Long paymentId;
    private Long bookingId;
    private BigDecimal amount;
    private String currency;
    private String checkoutUrl;
    private String sessionId;
    private String status;
}
