package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.PaymentReceival;
import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record PaymentReceivalResponse(
    Sid identifier,
    BigDecimal amount,
    String currency,
    LocalDate receivalDate,
    Optional<String> notes,
    Instant createdAt,
    PaymentReceival.ReceivalType receivalType) {}
