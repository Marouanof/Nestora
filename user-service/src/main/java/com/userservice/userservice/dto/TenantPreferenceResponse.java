package com.userservice.userservice.dto;

import com.userservice.userservice.entity.TenantPreference;
import com.userservice.userservice.enums.PropertyTypePreference;

import java.time.LocalDateTime;
import java.util.Set;

public record TenantPreferenceResponse(
        Long id,
        Long userId,
        String city,
        Set<PropertyTypePreference> propertyTypes,
        Double budgetMin,
        Double budgetMax,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TenantPreferenceResponse fromEntity(TenantPreference preference) {
        return new TenantPreferenceResponse(
                preference.getId(),
                preference.getUserId(),
                preference.getCity(),
                preference.getPropertyTypes(),
                preference.getBudgetMin(),
                preference.getBudgetMax(),
                preference.getCreatedAt(),
                preference.getUpdatedAt()
        );
    }
}
