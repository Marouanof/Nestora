package com.example.bookingservice.service;

import com.example.bookingservice.dto.BookingResponse;
import com.example.bookingservice.enu.BookingStatus;
import com.example.bookingservice.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminBookingService {

    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public Page<BookingResponse> getAllBookings(Pageable pageable) {
        return bookingRepository.findAll(pageable).map(booking -> BookingResponse.builder()
                .id(booking.getId())
                .propertyId(booking.getPropertyId())
                .tenantId(booking.getTenantId())
                .ownerId(booking.getOwnerId())
                .checkIn(booking.getCheckIn())
                .checkOut(booking.getCheckOut())
                .numberOfGuests(booking.getNumberOfGuests())
                .totalPrice(booking.getTotalPrice())
                .securityDeposit(booking.getSecurityDeposit())
                .status(booking.getStatus())
                .cancellationReason(booking.getCancellationReason())
                .createdAt(booking.getCreatedAt())
                .confirmedAt(booking.getConfirmedAt())
                .cancelledAt(booking.getCancelledAt())
                .completedAt(booking.getCompletedAt())
                .paymentConfirmedAt(booking.getPaymentConfirmedAt())
                .build());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBookingStats() {
        long total = bookingRepository.count();
        return Map.of(
                "totalBookings", total,
                "activeBookings", bookingRepository.countByStatus(BookingStatus.ACTIVE),
                "confirmedBookings", bookingRepository.countByStatus(BookingStatus.CONFIRMED),
                "completedBookings", bookingRepository.countByStatus(BookingStatus.COMPLETED),
                "cancelledBookings", bookingRepository.countByStatus(BookingStatus.CANCELLED),
                "pendingPaymentBookings", bookingRepository.countByStatus(BookingStatus.PENDING_PAYMENT),
                "disputedBookings", bookingRepository.countByStatus(BookingStatus.DISPUTED)
        );
    }
}
