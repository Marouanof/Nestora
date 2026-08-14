package com.example.bookingservice.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingPaymentEvent {
    private Long bookingId;
    private boolean success;
    private String reason;
}
