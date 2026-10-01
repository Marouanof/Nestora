package com.propertyservice.propertyservice.service;

import com.propertyservice.propertyservice.dto.PriceCalculationResult;
import com.propertyservice.propertyservice.entity.AvailabilityCalendar;
import com.propertyservice.propertyservice.entity.Property;
import com.propertyservice.propertyservice.enu.AvailabilityStatus;
import com.propertyservice.propertyservice.enu.ListingStatus;
import com.propertyservice.propertyservice.exception.PropertyNotFoundException;
import com.propertyservice.propertyservice.repository.AvailabilityRepository;
import com.propertyservice.propertyservice.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final PropertyRepository propertyRepository;

    public boolean isPropertyAvailable(Long propertyId, LocalDate startDate, LocalDate endDate) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException("Property not found with id: " + propertyId));

        if (property.getStatus() != ListingStatus.ACTIVE) {
            return false;
        }

        validateDates(startDate, endDate);

        return areDatesAvailable(propertyId, startDate, endDate);
    }

    @Transactional
    public void blockDates(Long propertyId, List<LocalDate> dates, Long ownerId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException("Property not found"));

        if (!property.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized to block dates for this property");
        }

        for (LocalDate date : dates) {
            if (date.isBefore(LocalDate.now())) {
                continue;
            }

            AvailabilityCalendar availability = availabilityRepository
                    .findByPropertyIdAndDate(propertyId, date)
                    .orElse(AvailabilityCalendar.builder()
                            .property(property)
                            .date(date)
                            .status(AvailabilityStatus.AVAILABLE)
                            .build());

            availability.setStatus(AvailabilityStatus.BLOCKED);
            availabilityRepository.save(availability);
        }

        log.info("Dates blocked for property {} by user {}", propertyId, ownerId);
    }

    @Transactional
    public void unblockDates(Long propertyId, List<LocalDate> dates, Long ownerId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException("Property not found"));

        if (!property.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized to unblock dates for this property");
        }

        for (LocalDate date : dates) {
                availabilityRepository.findByPropertyIdAndDate(propertyId, date)
                    .ifPresent(availability -> {
                        // Ne libérer que les blocages owner, jamais les réservations guests
                        if (availability.getStatus() == AvailabilityStatus.BLOCKED) {
                            availability.setStatus(AvailabilityStatus.AVAILABLE);
                            availabilityRepository.save(availability);
                        }
                    });
        }

        log.info("Dates unblocked for property {} by user {}", propertyId, ownerId);
    }

    // Nuits facturées/verrouillées : [startDate, endDate) — le jour du check-out reste réservable
    private boolean areDatesAvailable(Long propertyId, LocalDate startDate, LocalDate endDate) {
        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            Optional<AvailabilityCalendar> calendarEntry =
                    availabilityRepository.findByPropertyIdAndDate(propertyId, date);

            // Si une entrée existe, vérifier son statut
            if (calendarEntry.isPresent()) {
                AvailabilityStatus status = calendarEntry.get().getStatus();
                if (status == AvailabilityStatus.LOCKED ||
                        status == AvailabilityStatus.BOOKED ||
                        status == AvailabilityStatus.BLOCKED) {
                    return false; // Date non disponible
                }
            }
        }
        return true;
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }
    }

    @Transactional
    public String lockDatesForReservation(Long propertyId, LocalDate startDate,
                                          LocalDate endDate, Long tenantId) {

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException("Property not found"));

        validateDates(startDate, endDate);

        long numberOfNights = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (numberOfNights < property.getMinStayNights()) {
            throw new IllegalArgumentException(
                    "Minimum stay not satisfied. Minimum nights: " + property.getMinStayNights() +
                            ", requested: " + numberOfNights);
        }

        if (!areDatesAvailable(propertyId, startDate, endDate)) {
            throw new RuntimeException("Dates not available for reservation");
        }

        String lockToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            AvailabilityCalendar availability = availabilityRepository
                    .findByPropertyIdAndDate(propertyId, date)
                    .orElse(AvailabilityCalendar.builder()
                            .property(property)
                            .date(date)
                            .status(AvailabilityStatus.AVAILABLE)
                            .build());


            availability.setStatus(AvailabilityStatus.LOCKED);
            availability.setLockToken(lockToken);
            availability.setLockExpiresAt(expiresAt);
            availability.setTenantId(tenantId);

            availabilityRepository.save(availability);
        }

        log.info("🔐 Dates locked: property={}, tenant={}, token={}, dates={} to {}.",
                propertyId, tenantId, lockToken, startDate, endDate);
        return lockToken;
    }

    public List<LocalDate> getUnavailableDates(Long propertyId, LocalDate start, LocalDate end) {
        return availabilityRepository.findByPropertyIdAndDateBetween(propertyId, start, end)
                .stream()
                .filter(a -> a.getStatus() == AvailabilityStatus.BOOKED
                        || a.getStatus() == AvailabilityStatus.LOCKED
                        || a.getStatus() == AvailabilityStatus.BLOCKED)
                .map(AvailabilityCalendar::getDate)
                .toList();
    }

    /**
     * Méthode appelée par Booking-Service si le paiement échoue ou timeout
     * Booking-Service fera : POST /api/properties/{id}/availability/release/{token}
     */
    @Transactional
    public void releaseDates(Long propertyId, String lockToken) {
        List<AvailabilityCalendar> lockedDates =
                availabilityRepository.findByPropertyIdAndLockToken(propertyId, lockToken);

        for (AvailabilityCalendar date : lockedDates) {
            if (date.getStatus() == AvailabilityStatus.LOCKED) {
                // Si c'était encore en attente, on remet disponible
                date.setStatus(AvailabilityStatus.AVAILABLE);
            }
            date.setLockToken(null);
            date.setLockExpiresAt(null);
            date.setTenantId(null);
            availabilityRepository.save(date);
        }

        log.info("✅ Dates released: property={}, token={}", propertyId, lockToken);
    }

    /**
     * Nettoyage automatique des verrous expirés (toutes les minutes)
     * Les dates reviennent automatiquement disponibles après 15 min
     */
    @Scheduled(fixedRate = 60000) // 60,000 ms = 1 minute
    @Transactional
    public void cleanupExpiredLocks() {
        LocalDateTime now = LocalDateTime.now();
        List<AvailabilityCalendar> expiredLocks =
                availabilityRepository.findByLockExpiresAtBefore(now);

        if (!expiredLocks.isEmpty()) {
            for (AvailabilityCalendar date : expiredLocks) {
                date.setStatus(AvailabilityStatus.AVAILABLE);
                date.setLockToken(null);
                date.setLockExpiresAt(null);
                date.setTenantId(null);
                availabilityRepository.save(date);
            }
            log.info("🧹 Cleaned up {} expired locks", expiredLocks.size());
        }
    }

    /**
     * Calcule le prix total pour une période en appliquant les priceMultiplier
     * du calendrier (tarifs saisonniers/week-end définis par l'owner).
     * Les nuits sans entrée calendaire sont facturées au prix de base.
     * Nuits facturées : [startDate, endDate) — le jour de départ n'est pas facturé.
     */
    public BigDecimal calculateTotalPrice(Long propertyId, LocalDate startDate, LocalDate endDate) {
        // Vérifier que la propriété existe
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException(
                        "Property not found with id: " + propertyId));

        // Validation des dates
        validateDates(startDate, endDate);

        // Calculer le nombre de nuits
        long numberOfNights = ChronoUnit.DAYS.between(startDate, endDate);

        if (numberOfNights <= 0) {
            throw new IllegalArgumentException("Invalid date range: end date must be after start date");
        }

        // Vérifier le séjour minimum
        if (numberOfNights < property.getMinStayNights()) {
            throw new RuntimeException(
                    "Minimum stay not satisfied. Minimum nights: " + property.getMinStayNights() +
                            ", requested: " + numberOfNights);
        }

        // Multiplicateurs par date (dernière nuit facturée = endDate - 1)
        java.util.Map<LocalDate, BigDecimal> multiplierByDate = new java.util.HashMap<>();
        for (AvailabilityCalendar entry : availabilityRepository.findByPropertyIdAndDateBetween(
                propertyId, startDate, endDate.minusDays(1))) {
            if (entry.getPriceMultiplier() != null && entry.getPriceMultiplier().signum() > 0) {
                multiplierByDate.put(entry.getDate(), entry.getPriceMultiplier());
            }
        }

        BigDecimal totalPrice = BigDecimal.ZERO;
        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            BigDecimal multiplier = multiplierByDate.getOrDefault(date, BigDecimal.ONE);
            totalPrice = totalPrice.add(property.getPricePerNight().multiply(multiplier));
        }

        log.info("💰 Price Quote: property={}, nights={}, base/night={}, adjusted multipliers={}, total={}",
                propertyId, numberOfNights, property.getPricePerNight(),
                multiplierByDate.size(), totalPrice);

        return totalPrice;
    }

    public PriceCalculationResult calculatePriceWithAvailability(
            Long propertyId, LocalDate startDate, LocalDate endDate, Long tenantId) {

        // 1. Vérifier disponibilité
        boolean isAvailable = isPropertyAvailable(propertyId, startDate, endDate);

        if (!isAvailable) {
            throw new RuntimeException("Property not available for these dates");
        }

        // 2. Calculer prix
        BigDecimal totalPrice = calculateTotalPrice(propertyId, startDate, endDate);

        // 3. Vérifier séjour minimum
        Property property = propertyRepository.findById(propertyId).orElseThrow();
        long numberOfNights = ChronoUnit.DAYS.between(startDate, endDate);

        return PriceCalculationResult.builder()
                .propertyId(propertyId)
                .startDate(startDate)
                .endDate(endDate)
                .numberOfNights((int) numberOfNights)
                .pricePerNight(property.getPricePerNight())
                .totalPrice(totalPrice)
                .securityDeposit(property.getSecurityDeposit())
                .isAvailable(true)
                .minStayNights(property.getMinStayNights())
                .build();
    }
    @Transactional
    public void generateAvailabilityForYear(Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException("Property not found"));

        LocalDate today = LocalDate.now();
        List<AvailabilityCalendar> calendars = new java.util.ArrayList<>();

        for (int i = 0; i < 365; i++) {
            LocalDate date = today.plusDays(i);
            // Check if already exists to avoid duplicates if re-run
            if (availabilityRepository.findByPropertyIdAndDate(propertyId, date).isEmpty()) {
                calendars.add(AvailabilityCalendar.builder()
                        .property(property)
                        .date(date)
                        .status(AvailabilityStatus.AVAILABLE)
                        .priceMultiplier(BigDecimal.ONE) // Default multiplier
                        .build());
            }
        }

        availabilityRepository.saveAll(calendars);
        log.info("Generated availability for property {} for the next 365 days", propertyId);
    }

    /**
     * Méthode appelée par Booking-Service après confirmation de réservation.
     * Bloque définitivement les dates (status BOOKED).
     * Vérifie que le verrou existe et correspond au token fourni.
     *
     * @param propertyId ID de la propriété
     * @param startDate Date de début
     * @param endDate Date de fin
     * @param lockToken Token de verrouillage à valider
     */
    @Transactional
    public void blockDatesForBooking(Long propertyId, LocalDate startDate, LocalDate endDate, String lockToken) {
        log.info("🔗 Processing booking confirmation: property={}, token={}", propertyId, lockToken);

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException("Property not found with id: " + propertyId));

        if (property.getStatus() != ListingStatus.ACTIVE) {
            throw new RuntimeException("Cannot book an inactive property");
        }

        // Dates optionnelles : la confirmation se fait par token
        if (startDate != null && endDate != null) {
            validateDates(startDate, endDate);
        }

        // Récupérer les entrées verrouillées avec ce token
        List<AvailabilityCalendar> lockedDates = availabilityRepository.findByPropertyIdAndLockToken(propertyId, lockToken);

        if (lockedDates.isEmpty()) {
            log.error("❌ Booking confirmation failed validation: No dates found for token {}", lockToken);
            throw new RuntimeException("Invalid or expired lock token: " + lockToken);
        }

        // Vérification de l'expiration du verrou sur la première date trouvée (car elles partagent le même token/expiration)
        LocalDateTime now = LocalDateTime.now();
        if (lockedDates.get(0).getLockExpiresAt() != null && lockedDates.get(0).getLockExpiresAt().isBefore(now)) {
            log.error("❌ Booking confirmation received for expired lock token {}", lockToken);
            throw new RuntimeException("Lock token has expired. Please retry the booking flow.");
        }

        // Marquer comme BOOKED et nettoyer les champs de verrouillage
        for (AvailabilityCalendar date : lockedDates) {
            date.setStatus(AvailabilityStatus.BOOKED);
            date.setLockToken(null);
            date.setLockExpiresAt(null);
            // On garde le tenantId pour l'historique de qui a réservé
            availabilityRepository.save(date);
        }

        log.info("✅ Booking confirmed! Dates blocked definitively for property {}.", propertyId);
    }
}
