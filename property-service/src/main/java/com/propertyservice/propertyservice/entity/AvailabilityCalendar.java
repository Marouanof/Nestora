package com.propertyservice.propertyservice.entity;

import com.propertyservice.propertyservice.enu.AvailabilityStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "availability_calendar",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"property_id", "date"})
        },
        indexes = {
                @Index(name = "idx_availability_lock_token", columnList = "lock_token"),
                @Index(name = "idx_availability_lock_expires", columnList = "lock_expires_at")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityCalendar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AvailabilityStatus status = AvailabilityStatus.AVAILABLE;

    @Column(name = "lock_token")
    private String lockToken; // Token unique pour identifier ce verrou

    @Column(name = "lock_expires_at")
    private LocalDateTime lockExpiresAt; // Date d'expiration du verrou (1h)

    @Column(name = "tenant_id")
    private Long tenantId; // ID du locataire qui veut réserver

    @Column(name = "price_multiplier", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal priceMultiplier = BigDecimal.ONE;
}
