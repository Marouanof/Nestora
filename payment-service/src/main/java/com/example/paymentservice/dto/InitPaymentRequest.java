package com.example.paymentservice.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class InitPaymentRequest {
    private Long bookingId;
}
