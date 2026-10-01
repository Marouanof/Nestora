package com.userservice.userservice.service;

import com.userservice.userservice.dto.OwnerListingPreferenceRequest;
import com.userservice.userservice.dto.OwnerListingPreferenceResponse;
import com.userservice.userservice.entity.OwnerListingPreference;
import com.userservice.userservice.repository.OwnerListingPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OwnerListingPreferenceService {

    private final OwnerListingPreferenceRepository repository;

    @Transactional(readOnly = true)
    public OwnerListingPreferenceResponse getPreferences(Long userId) {
        return repository.findByUserId(userId)
                .map(OwnerListingPreferenceResponse::fromEntity)
                .orElse(null);
    }

    @Transactional
    public OwnerListingPreferenceResponse savePreferences(Long userId, OwnerListingPreferenceRequest request) {
        OwnerListingPreference preference = repository.findByUserId(userId)
                .orElseGet(() -> {
                    OwnerListingPreference p = new OwnerListingPreference();
                    p.setUserId(userId);
                    return p;
                });

        preference.setListingTypes(
                request.listingTypes() != null ? request.listingTypes() : java.util.Set.of());

        return OwnerListingPreferenceResponse.fromEntity(repository.save(preference));
    }
}
