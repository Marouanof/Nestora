package com.userservice.userservice.dto;

import java.util.List;

public record WishlistResponse(
        List<Long> propertyIds,
        List<PropertyResponseDTO> properties
) {
}
