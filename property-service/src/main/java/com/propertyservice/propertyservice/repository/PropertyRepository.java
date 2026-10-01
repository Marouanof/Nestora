package com.propertyservice.propertyservice.repository;

import com.propertyservice.propertyservice.enu.ListingStatus;
import com.propertyservice.propertyservice.enu.PropertyType;
import com.propertyservice.propertyservice.entity.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {

    // Seed de démo : existence par titre (idempotent et additif)
    Optional<Property> findByTitle(String title);

    // Trouver les propriétés d'un owner
    Page<Property> findByOwnerId(Long ownerId, Pageable pageable);

    List<Property> findByOwnerId(Long ownerId);

    Page<Property> findByStatus(ListingStatus status, Pageable pageable);

    long countByStatus(ListingStatus status);

    // Compter par type
    long countByTypeAndStatus(PropertyType type, ListingStatus status);
}
