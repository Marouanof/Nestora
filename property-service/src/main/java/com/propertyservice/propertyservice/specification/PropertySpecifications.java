package com.propertyservice.propertyservice.specification;

import com.propertyservice.propertyservice.entity.Property;
import com.propertyservice.propertyservice.enu.ListingStatus;
import com.propertyservice.propertyservice.enu.AvailabilityStatus;
import com.propertyservice.propertyservice.enu.PropertyType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PropertySpecifications {
    public static Specification<Property> isAvailable() {
        return (root, query, cb) ->
                cb.equal(root.get("status"), ListingStatus.ACTIVE);
    }

    // Type de propriété
    public static Specification<Property> hasPropertyType(PropertyType type) {
        return (root, query, cb) -> {
            if (type == null) return null;
            return cb.equal(root.get("type"), type);
        };
    }

    // Nombre de voyageurs
    public static Specification<Property> canAccommodateGuests(Integer guests) {
        return (root, query, cb) -> {
            if (guests == null || guests <= 0) return null;
            return cb.greaterThanOrEqualTo(root.get("maxGuests"), guests);
        };
    }

    // Nombre de chambres
    public static Specification<Property> hasMinBedrooms(Integer bedrooms) {
        return (root, query, cb) -> {
            if (bedrooms == null || bedrooms <= 0) return null;
            return cb.greaterThanOrEqualTo(root.get("bedrooms"), bedrooms);
        };
    }

    // Nombre de salles de bain
    public static Specification<Property> hasMinBathrooms(Integer bathrooms) {
        return (root, query, cb) -> {
            if (bathrooms == null || bathrooms <= 0) return null;
            return cb.greaterThanOrEqualTo(root.get("bathrooms"), bathrooms);
        };
    }

    // Prix minimum
    public static Specification<Property> hasMinPrice(BigDecimal minPrice) {
        return (root, query, cb) -> {
            if (minPrice == null || minPrice.compareTo(BigDecimal.ZERO) <= 0) return null;
            return cb.greaterThanOrEqualTo(root.get("pricePerNight"), minPrice);
        };
    }

    // Prix maximum
    public static Specification<Property> hasMaxPrice(BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (maxPrice == null || maxPrice.compareTo(BigDecimal.ZERO) <= 0) return null;
            return cb.lessThanOrEqualTo(root.get("pricePerNight"), maxPrice);
        };
    }

    // Réservation instantanée
    public static Specification<Property> isInstantBookable(Boolean instantBookable) {
        return (root, query, cb) -> {
            if (instantBookable == null) return null;
            return cb.equal(root.get("instantBookable"), instantBookable);
        };
    }

    // Équipements : le bien doit posséder TOUS les équipements demandés (insensible à la casse)
    public static Specification<Property> hasAmenities(List<String> amenities) {
        return (root, query, cb) -> {
            if (amenities == null || amenities.isEmpty()) return null;

            query.distinct(true); // les joins multiplient les lignes

            List<Predicate> predicates = new ArrayList<>();
            for (String amenity : amenities) {
                if (amenity == null || amenity.isBlank()) continue;
                predicates.add(cb.equal(
                        cb.lower(root.join("amenities")),
                        amenity.trim().toLowerCase()));
            }

            if (predicates.isEmpty()) return null;
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Géo-recherche : bounding box autour du point (pré-filtre SQL, filtrage exact haversine côté service)
    public static Specification<Property> withinBoundingBox(Double lat, Double lon, Double radiusKm) {
        return (root, query, cb) -> {
            if (lat == null || lon == null || radiusKm == null || radiusKm <= 0) return null;

            var latitude = root.get("address").get("latitude").as(Double.class);
            var longitude = root.get("address").get("longitude").as(Double.class);

            double latDelta = radiusKm / 111.32;
            double lonDelta = radiusKm / (111.32 * Math.max(Math.cos(Math.toRadians(lat)), 0.01));

            return cb.and(
                    cb.isNotNull(latitude),
                    cb.isNotNull(longitude),
                    cb.between(latitude, lat - latDelta, lat + latDelta),
                    cb.between(longitude, lon - lonDelta, lon + lonDelta)
            );
        };
    }

    // Disponibilité entre dates (IMPORTANT !)
    public static Specification<Property> availableBetween(LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> {
            if (startDate == null || endDate == null) return null;

            // Sous-requête pour vérifier disponibilités
            var subquery = query.subquery(Long.class);
            var availabilityRoot = subquery.from(com.propertyservice.propertyservice.entity.AvailabilityCalendar.class);

            subquery.select(cb.count(availabilityRoot));
            subquery.where(
                    cb.and(
                            cb.equal(availabilityRoot.get("property").get("id"), root.get("id")),
                            cb.equal(availabilityRoot.get("status"), AvailabilityStatus.AVAILABLE),
                            cb.between(availabilityRoot.get("date"), startDate, endDate.minusDays(1))
                    )
            );

            // Doit avoir toutes les dates disponibles
            long requiredDays = startDate.until(endDate).getDays();
            return cb.equal(subquery, requiredDays);
        };
    }

    // Note minimum (complexe - besoin de join avec reviews)
    public static Specification<Property> hasMinRating(Double minRating) {
        return (root, query, cb) -> {
            if (minRating == null || minRating < 0 || minRating > 5) return null;

            query.distinct(true); // Éviter les doublons

            // Sous-requête pour la moyenne des reviews
            var subquery = query.subquery(Double.class);
            var reviewRoot = subquery.from(com.propertyservice.propertyservice.entity.Review.class);

            subquery.select(cb.avg(reviewRoot.get("rating")));
            subquery.where(cb.equal(reviewRoot.get("property").get("id"), root.get("id")));

            return cb.greaterThanOrEqualTo(subquery, minRating);
        };
    }

    public static Specification<Property> inLocation(String location) {
        return (root, query, cb) -> {
            if (location == null || location.isBlank()) return null;

            String searchPattern = "%" + location.toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("address").get("city")), searchPattern),
                    cb.like(cb.lower(root.get("address").get("country")), searchPattern),
                    cb.like(cb.lower(root.get("title")), searchPattern)
            );
        };
    }
}
