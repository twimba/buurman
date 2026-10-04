package com.buurman.dto.request;

import java.math.BigDecimal;
import java.util.Optional;

import com.buurman.domain.identifier.ContactCreditIdentifier;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@SkipTestCoverage
public record ApplyCreditRequest(
    @NotNull(message = "Credit identifier is required") ContactCreditIdentifier creditIdentifier,
    // Amount to apply; defaults to min(remaining credit, open balance).
    Optional<@Positive(message = "Amount must be positive") BigDecimal> amount) {}
