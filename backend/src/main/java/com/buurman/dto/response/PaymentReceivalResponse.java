package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PaymentReceivalResponse(
        String identifier,
        BigDecimal amount,
        LocalDate receivalDate,
        String notes,
        Instant createdAt
) {
}
