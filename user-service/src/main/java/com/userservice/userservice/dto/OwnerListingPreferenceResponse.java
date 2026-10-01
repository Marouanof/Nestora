package com.userservice.userservice.dto;

import com.userservice.userservice.entity.OwnerListingPreference;
import com.userservice.userservice.enums.ListingTypePreference;

import java.time.LocalDateTime;
import java.util.Set;

public record OwnerListingPreferenceResponse(
        Long id,
        Long userId,
        Set<ListingTypePreference> listingTypes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OwnerListingPreferenceResponse fromEntity(OwnerListingPreference preference) {
        return new OwnerListingPreferenceResponse(
                preference.getId(),
                preference.getUserId(),
                preference.getListingTypes(),
                preference.getCreatedAt(),
                preference.getUpdatedAt()
        );
    }
}
