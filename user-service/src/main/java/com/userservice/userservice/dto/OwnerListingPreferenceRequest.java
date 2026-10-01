package com.userservice.userservice.dto;

import com.userservice.userservice.enums.ListingTypePreference;

import java.util.Set;

public record OwnerListingPreferenceRequest(
        Set<ListingTypePreference> listingTypes
) {
}
