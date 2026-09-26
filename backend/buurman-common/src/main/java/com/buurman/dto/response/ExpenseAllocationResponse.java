package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.AllocationBasis;
import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

/** One unit's stored share of a building-level expense. */
@SkipTestCoverage
public record ExpenseAllocationResponse(
    Sid identifier,
    Sid unitIdentifier,
    String unitNumber,
    Optional<BigDecimal> amount,
    Optional<String> amountCurrency,
    AllocationBasis basis,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
