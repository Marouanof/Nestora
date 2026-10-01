package com.example.paymentservice.messaging;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingPaymentEvent {
    private Long bookingId;
    private boolean success;
    private String reason;
}
