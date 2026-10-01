package com.propertyservice.propertyservice.repository;

import com.propertyservice.propertyservice.entity.AvailabilityCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AvailabilityRepository extends JpaRepository<AvailabilityCalendar, Long> {

    List<AvailabilityCalendar> findByPropertyIdAndDateBetween(Long propertyId, LocalDate startDate, LocalDate endDate);

    Optional<AvailabilityCalendar> findByPropertyIdAndDate(Long propertyId, LocalDate date);

    // Trouver les verrous d'une propriété spécifique avec un token
    List<AvailabilityCalendar> findByPropertyIdAndLockToken(Long propertyId, String lockToken);

    // Trouver les verrous expirés (pour nettoyage automatique)
    List<AvailabilityCalendar> findByLockExpiresAtBefore(LocalDateTime expiryTime);
}
