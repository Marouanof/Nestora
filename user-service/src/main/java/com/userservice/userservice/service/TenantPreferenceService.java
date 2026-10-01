package com.userservice.userservice.service;

import com.userservice.userservice.dto.TenantPreferenceRequest;
import com.userservice.userservice.dto.TenantPreferenceResponse;
import com.userservice.userservice.entity.TenantPreference;
import com.userservice.userservice.exception.InvalidActionException;
import com.userservice.userservice.repository.TenantPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantPreferenceService {

    private final TenantPreferenceRepository tenantPreferenceRepository;

    @Transactional(readOnly = true)
    public TenantPreferenceResponse getPreferences(Long userId) {
        return tenantPreferenceRepository.findByUserId(userId)
                .map(TenantPreferenceResponse::fromEntity)
                .orElse(null);
    }

    @Transactional
    public TenantPreferenceResponse savePreferences(Long userId, TenantPreferenceRequest request) {
        validateBudget(request);

        TenantPreference preference = tenantPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    TenantPreference p = new TenantPreference();
                    p.setUserId(userId);
                    return p;
                });

        preference.setCity(request.city());
        preference.setPropertyTypes(
                request.propertyTypes() != null ? request.propertyTypes() : java.util.Set.of());
        preference.setBudgetMin(request.budgetMin());
        preference.setBudgetMax(request.budgetMax());

        return TenantPreferenceResponse.fromEntity(tenantPreferenceRepository.save(preference));
    }

    private void validateBudget(TenantPreferenceRequest request) {
        if (request.budgetMin() != null && request.budgetMax() != null
                && request.budgetMin() > request.budgetMax()) {
            throw new InvalidActionException("Le budget minimum ne peut pas dépasser le budget maximum");
        }
    }
}
