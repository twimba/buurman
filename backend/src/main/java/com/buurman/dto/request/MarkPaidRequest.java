package com.buurman.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record MarkPaidRequest(
    @NotNull(message = "Payment date is required") LocalDate paymentDate, String notes) {}
