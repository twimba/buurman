package com.buurman.dto.request;

import java.math.BigDecimal;
import java.util.List;

import com.buurman.domain.identifier.UnitIdentifier;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * A caller-supplied, per-unit override of an expense's allocation (MANUAL basis). The entries'
 * amounts must sum to exactly the expense's total; a mismatch is rejected with a 409.
 */
@SkipTestCoverage
public record ManualAllocationRequest(
    @NotEmpty(message = "At least one allocation entry is required") @Valid List<ManualAllocationEntry> entries) {

  public record ManualAllocationEntry(
      @NotNull(message = "Unit identifier is required") UnitIdentifier unitIdentifier,
      @NotNull(message = "Amount is required") BigDecimal amount) {}
}
