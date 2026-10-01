package com.userservice.userservice.dto;

import jakarta.validation.constraints.NotBlank;

public record SendOtpRequest(
        @NotBlank String phone
) {
}
