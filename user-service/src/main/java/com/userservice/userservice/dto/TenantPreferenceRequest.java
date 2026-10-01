package com.userservice.userservice.dto;

import com.userservice.userservice.enums.PropertyTypePreference;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record TenantPreferenceRequest(

        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        Set<PropertyTypePreference> propertyTypes,

        @DecimalMin(value = "0", message = "Minimum budget must be positive")
        Double budgetMin,

        @DecimalMin(value = "0", message = "Maximum budget must be positive")
        Double budgetMax
) {

        @AssertTrue(message = "Minimum budget must be less than or equal to maximum budget")
        boolean isBudgetRangeValid() {
                return budgetMin == null || budgetMax == null || budgetMin <= budgetMax;
        }
}
