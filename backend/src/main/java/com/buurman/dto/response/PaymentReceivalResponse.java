package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentReceivalResponse(
        UUID id,
        String identifier,
        BigDecimal amount,
        LocalDate receivalDate,
        String notes,
        Instant createdAt
) {
}
