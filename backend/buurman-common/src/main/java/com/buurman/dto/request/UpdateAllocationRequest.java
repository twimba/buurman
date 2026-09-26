package com.buurman.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.AllocationBasis;
import com.buurman.domain.identifier.UnitIdentifier;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Sets a property's allocation basis and, for CUSTOM, each unit's share. Does not retroactively
 * rewrite any expense's already-persisted allocations — call the per-expense recompute endpoint for
 * that, explicitly, since settlement statements are legal documents.
 */
@SkipTestCoverage
public record UpdateAllocationRequest(
    @NotNull(message = "Allocation basis is required") AllocationBasis basis,
    Optional<@Valid List<UnitShareEntry>> shares) {

  public record UnitShareEntry(
      @NotNull(message = "Unit identifier is required") UnitIdentifier unitIdentifier,
      @NotNull(message = "Share percentage is required") @DecimalMin(value = "0", message = "Share percentage must be at least 0") @DecimalMax(value = "100", message = "Share percentage must be at most 100") BigDecimal sharePct) {}
}
