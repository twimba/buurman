package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

public record PaymentReceivalResponse(
    String identifier,
    BigDecimal amount,
    String currency,
    LocalDate receivalDate,
    Optional<String> notes,
    Instant createdAt) {}
