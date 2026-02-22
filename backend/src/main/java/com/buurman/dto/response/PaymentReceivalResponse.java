package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

public record PaymentReceivalResponse(
    String identifier,
    BigDecimal amount,
    String currency,
    LocalDate receivalDate,
    @Nullable String notes,
    Instant createdAt) {}
