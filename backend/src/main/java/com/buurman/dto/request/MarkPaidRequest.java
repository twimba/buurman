package com.buurman.dto.request;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.NotNull;

public record MarkPaidRequest(
    @NotNull(message = "Payment date is required") LocalDate paymentDate, @Nullable String notes) {}
