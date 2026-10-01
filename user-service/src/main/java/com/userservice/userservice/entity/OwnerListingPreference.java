package com.userservice.userservice.entity;

import com.userservice.userservice.enums.ListingTypePreference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "owner_listing_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OwnerListingPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "owner_listing_preference_types",
            joinColumns = @JoinColumn(name = "preference_id"))
    @Column(name = "listing_type")
    @Enumerated(EnumType.STRING)
    private Set<ListingTypePreference> listingTypes = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
