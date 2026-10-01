package com.propertyservice.propertyservice.dto;

import com.propertyservice.propertyservice.enu.PropertyType;
import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
@Data
public class SearchRequest {
    // 📍 Localisation (recherche texte)
    private String location; // Peut être ville, quartier, pays

    // 📅 Dates (optionnel)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkIn;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOut;

    // 👥 Nombre de voyageurs
    private Integer guests;

    // 🏠 Type de propriété (filtre)
    private PropertyType propertyType;

    // 💰 Fourchette de prix
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    // 🛏️ Nombre de chambres
    private Integer bedrooms;
    private Integer bathrooms;

    // ⭐ Note minimum (simple)
    private Integer minRating; // 1, 2, 3, 4, 5

    // ✅ Options
    private Boolean instantBookable;

    // 🏊 Équipements requis (le bien doit tous les avoir)
    private List<String> amenities;

    // 📍 Géo-recherche : rayon autour d'un point
    private Double latitude;
    private Double longitude;
    private Double radiusKm; // défaut : 50 km

    public boolean hasGeoSearch() {
        return latitude != null && longitude != null;
    }

    // 📋 Pagination (déjà géré par Spring)

    // Méthode utilitaire
    public boolean hasDates() {
        return checkIn != null && checkOut != null;
    }
}
