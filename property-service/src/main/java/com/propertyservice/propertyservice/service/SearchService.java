package com.propertyservice.propertyservice.service;

import com.propertyservice.propertyservice.dto.*;
import com.propertyservice.propertyservice.entity.Property;
import com.propertyservice.propertyservice.repository.PropertyRepository;
import com.propertyservice.propertyservice.specification.PropertySpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final PropertyRepository propertyRepository;
    private final PropertyService propertyService;

    @Transactional(readOnly = true)
    public SearchResponse searchProperties(SearchRequest request, Pageable pageable) {
        log.info("🔍 Searching properties with filters: {}", request);

        boolean geoSearch = request.hasGeoSearch();
        final double effectiveRadius =
                request.getRadiusKm() != null && request.getRadiusKm() > 0 ? request.getRadiusKm() : 50.0;

        // 1. Construire la specification avec tous les filtres
        Specification<Property> spec = buildSpecification(request);

        // 2. Exécuter la recherche
        Page<Property> propertiesPage = propertyRepository.findAll(spec, pageable);

        // 3. Convertir en PropertyResponse
        java.util.List<PropertyResponse> responses = new java.util.ArrayList<>(
                propertiesPage.map(propertyService::mapToPropertyResponse).getContent());

        // 4. Géo-recherche : filtrage exact haversine + tri par distance dans la page
        if (geoSearch) {
            responses = responses.stream()
                    .map(r -> attachDistance(r, request.getLatitude(), request.getLongitude()))
                    .filter(r -> r.getDistanceKm() != null && r.getDistanceKm() <= effectiveRadius)
                    .sorted(java.util.Comparator.comparingDouble(PropertyResponse::getDistanceKm))
                    .toList();
        }

        Page<PropertyResponse> propertyResponses =
                new org.springframework.data.domain.PageImpl<>(responses, pageable, propertiesPage.getTotalElements());

        return SearchResponse.builder()
                .properties(propertyResponses)
                .totalProperties(geoSearch ? responses.size() : propertiesPage.getTotalElements())
                .currentPage(propertiesPage.getNumber())
                .totalPages(propertiesPage.getTotalPages())
                .build();
    }

    private Specification<Property> buildSpecification(SearchRequest request) {
        Specification<Property> spec = PropertySpecifications.isAvailable();

        // 1. Recherche par localisation
        if (request.getLocation() != null && !request.getLocation().isBlank()) {
            spec = spec.and(PropertySpecifications.inLocation(request.getLocation()));
        }

        // 2. Dates disponibles
        if (request.hasDates()) {
            spec = spec.and(PropertySpecifications.availableBetween(
                    request.getCheckIn(), request.getCheckOut()));
        }

        // 3. Nombre de voyageurs
        if (request.getGuests() != null && request.getGuests() > 0) {
            spec = spec.and(PropertySpecifications.canAccommodateGuests(request.getGuests()));
        }

        // 4. Type de propriété
        if (request.getPropertyType() != null) {
            spec = spec.and(PropertySpecifications.hasPropertyType(request.getPropertyType()));
        }

        // 5. Prix
        if (request.getMinPrice() != null && request.getMinPrice().compareTo(BigDecimal.ZERO) > 0) {
            spec = spec.and(PropertySpecifications.hasMinPrice(request.getMinPrice()));
        }

        if (request.getMaxPrice() != null && request.getMaxPrice().compareTo(BigDecimal.ZERO) > 0) {
            spec = spec.and(PropertySpecifications.hasMaxPrice(request.getMaxPrice()));
        }

        // 6. Chambres
        if (request.getBedrooms() != null && request.getBedrooms() > 0) {
            spec = spec.and(PropertySpecifications.hasMinBedrooms(request.getBedrooms()));
        }

        // ✅ 7. Salles de bain (AJOUTÉ)
        if (request.getBathrooms() != null && request.getBathrooms() > 0) {
            spec = spec.and(PropertySpecifications.hasMinBathrooms(request.getBathrooms()));
        }

        // ✅ 8. Note minimum (DÉCOMMENTÉ)
        if (request.getMinRating() != null && request.getMinRating() >= 1 && request.getMinRating() <= 5) {
            spec = spec.and(PropertySpecifications.hasMinRating(request.getMinRating().doubleValue()));
        }

        // 9. Réservation instantanée
        if (request.getInstantBookable() != null) {
            spec = spec.and(PropertySpecifications.isInstantBookable(request.getInstantBookable()));
        }

        // 10. Équipements requis
        if (request.getAmenities() != null && !request.getAmenities().isEmpty()) {
            spec = spec.and(PropertySpecifications.hasAmenities(request.getAmenities()));
        }

        // 11. Géo-recherche : pré-filtre bounding box en SQL
        if (request.hasGeoSearch()) {
            double radiusKm = request.getRadiusKm() != null && request.getRadiusKm() > 0 ? request.getRadiusKm() : 50.0;
            spec = spec.and(PropertySpecifications.withinBoundingBox(
                    request.getLatitude(), request.getLongitude(), radiusKm));
        }

        return spec;
    }

    private PropertyResponse attachDistance(PropertyResponse response, Double lat, Double lon) {
        var address = response.getAddress();
        if (address == null || address.getLatitude() == null || address.getLongitude() == null) {
            return response;
        }
        response.setDistanceKm(haversineKm(lat, lon, address.getLatitude(), address.getLongitude()));
        return response;
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
