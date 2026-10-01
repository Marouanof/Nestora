package com.example.bookingservice.controller;

import com.example.bookingservice.entity.Booking;
import com.example.bookingservice.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/internal/bookings")
@RequiredArgsConstructor
public class InternalBookingController {

    private final BookingRepository bookingRepository;

    @GetMapping("/{id}")
    public Booking getBookingById(@PathVariable Long id) {
        log.info("Internal request for booking ID: {}", id);
        return bookingRepository.findById(id)
                .orElseThrow(() -> new com.example.bookingservice.exception.BookingNotFoundException("Booking not found with id: " + id));
    }

    // Vérifie qu'un utilisateur a bien séjourné dans une propriété (reviews vérifiées)
    @GetMapping("/completed/exists")
    public boolean hasCompletedStay(
            @org.springframework.web.bind.annotation.RequestParam Long propertyId,
            @org.springframework.web.bind.annotation.RequestParam Long userId) {
        return bookingRepository.existsByPropertyIdAndTenantIdAndStatus(
                propertyId, userId, com.example.bookingservice.enu.BookingStatus.COMPLETED);
    }
}
