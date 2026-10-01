package com.userservice.userservice.kyc.provider;

public record ProviderResponse(
        String providerReference,
        boolean success,
        String status,
        String message
) {
    public static ProviderResponse ok(String providerReference, String message) {
        return new ProviderResponse(providerReference, true, null, message);
    }

    public static ProviderResponse failed(String providerReference, String message) {
        return new ProviderResponse(providerReference, false, null, message);
    }
}
