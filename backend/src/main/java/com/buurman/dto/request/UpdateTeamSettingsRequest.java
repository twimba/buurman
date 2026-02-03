package com.buurman.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateTeamSettingsRequest(
    @Valid PaymentSettings payments
) {
    public record PaymentSettings(
        @NotNull(message = "Payments ahead count is required")
        @Min(value = 1, message = "Payments ahead count must be at least 1")
        @Max(value = 12, message = "Payments ahead count must not exceed 12")
        Integer paymentsAheadCount,

        @NotNull(message = "Auto generation enabled flag is required")
        Boolean autoGenerationEnabled
    ) {}
}
