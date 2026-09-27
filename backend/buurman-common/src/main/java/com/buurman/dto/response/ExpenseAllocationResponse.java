package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.AllocationBasis;
import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

/**
 * One unit's stored share of a building-level expense.
 *
 * <p>{@code basis} is what was actually used; {@code requestedBasis} is the property's allocation
 * basis at the time of this read. They differ when AREA or CUSTOM silently fell back to EQUAL
 * because a sibling unit was missing its weight — {@code warnings} then names which unit(s) forced
 * that fallback. Both are non-fatal: the split still happened, just not on the basis nominally
 * configured.
 */
@SkipTestCoverage
public record ExpenseAllocationResponse(
    Sid identifier,
    Sid unitIdentifier,
    String unitNumber,
    Optional<BigDecimal> amount,
    Optional<String> amountCurrency,
    AllocationBasis basis,
    AllocationBasis requestedBasis,
    List<String> warnings,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
