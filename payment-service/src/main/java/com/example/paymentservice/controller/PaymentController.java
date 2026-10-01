package com.example.paymentservice.controller;

import com.example.paymentservice.dto.*;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    @PostMapping("/init")
    public ResponseEntity<?> init(@RequestHeader("X-Auth-User-Id") Long userId,
                                  @RequestBody InitPaymentRequest request) {
        if (request == null || request.getBookingId() == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "bookingId requis"));
        }
        try {
            return ResponseEntity.ok(paymentService.initPayment(request.getBookingId(), userId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(502).body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<PaymentResponse>> byBooking(@PathVariable Long bookingId) {
        List<PaymentResponse> out = paymentRepository.findByBookingIdOrderByCreatedAtDesc(bookingId)
                .stream().map(this::toDto).toList();
        return ResponseEntity.ok(out);
    }

    private PaymentResponse toDto(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId()).bookingId(p.getBookingId())
                .amount(p.getAmount()).currency(p.getCurrency())
                .status(p.getStatus().name()).checkoutUrl(p.getCheckoutUrl())
                .build();
    }
}
