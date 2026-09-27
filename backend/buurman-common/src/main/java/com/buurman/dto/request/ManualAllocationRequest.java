package com.buurman.dto.request;

import java.math.BigDecimal;
import java.util.List;

import com.buurman.domain.identifier.UnitIdentifier;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A caller-supplied, per-unit override of an expense's allocation (MANUAL basis). The entries'
 * amounts must sum to exactly the expense's total; a mismatch is rejected with a 409.
 */
@SkipTestCoverage
public record ManualAllocationRequest(
    @NotEmpty(message = "At least one allocation entry is required") @Size(max = 200, message = "At most 200 allocation entries are allowed") @Valid List<ManualAllocationEntry> entries) {

  public record ManualAllocationEntry(
      @NotNull(message = "Unit identifier is required") UnitIdentifier unitIdentifier,
      @NotNull(message = "Amount is required") @Digits(
              integer = 13,
              fraction = 4,
              message = "Amount must have at most 13 integer digits and 4 decimal places")
          BigDecimal amount) {}
}
