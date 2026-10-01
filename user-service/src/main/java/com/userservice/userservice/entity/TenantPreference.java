package com.userservice.userservice.entity;

import com.userservice.userservice.enums.PropertyTypePreference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tenant_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    private String city;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tenant_preference_property_types",
            joinColumns = @JoinColumn(name = "preference_id"))
    @Column(name = "property_type")
    @Enumerated(EnumType.STRING)
    private Set<PropertyTypePreference> propertyTypes = new HashSet<>();

    @Column(name = "budget_min")
    private Double budgetMin;

    @Column(name = "budget_max")
    private Double budgetMax;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
