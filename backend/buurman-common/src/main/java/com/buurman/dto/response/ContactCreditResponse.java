package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.ContactCredit;
import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record ContactCreditResponse(
    Sid identifier,
    Sid contactIdentifier,
    Optional<Sid> contractIdentifier,
    BigDecimal amount,
    BigDecimal remainingAmount,
    String currency,
    ContactCredit.CreditSource source,
    Optional<String> reason,
    Optional<Sid> sourcePaymentIdentifier,
    Optional<Instant> refundedAt,
    Optional<String> refundNotes,
    Instant createdAt) {}
