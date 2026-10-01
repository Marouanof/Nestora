package com.propertyservice.propertyservice.enu;

public enum AvailabilityStatus {
    AVAILABLE,
    LOCKED,     // Réservation en cours (token de verrouillage)
    BOOKED,     // Réservation confirmée d'un guest
    BLOCKED     // Blocage manuel par l'owner
}
