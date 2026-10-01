package com.userservice.userservice.dto;

import java.util.List;

public record CompletenessResponse(
        int percent,
        boolean kycComplete,
        boolean emailVerified,
        List<String> missing,
        List<NextAction> nextActions
) {
    public record NextAction(
            String code,
            String label,
            String href,
            boolean blockingForBooking
    ) {
    }
}
