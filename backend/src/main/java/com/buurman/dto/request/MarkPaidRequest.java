package com.buurman.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MarkPaidRequest(
        @NotNull(message = "Payment date is required")
        LocalDate paymentDate,

        String notes
) {
}
