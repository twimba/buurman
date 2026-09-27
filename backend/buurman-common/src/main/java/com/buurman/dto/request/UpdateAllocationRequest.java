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
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Sets a property's allocation basis and, for CUSTOM, each unit's share. Does not retroactively
 * rewrite any expense's already-persisted allocations — call the per-expense recompute endpoint for
 * that, explicitly, since settlement statements are legal documents.
 */
@SkipTestCoverage
public record UpdateAllocationRequest(
    @NotNull(message = "Allocation basis is required") AllocationBasis basis,
    Optional<
            @Valid @Size(max = 200, message = "At most 200 unit shares are allowed") List<
                UnitShareEntry>>
        shares) {

  public record UnitShareEntry(
      @NotNull(message = "Unit identifier is required") UnitIdentifier unitIdentifier,
      @NotNull(message = "Share percentage is required") @DecimalMin(value = "0", message = "Share percentage must be at least 0") @DecimalMax(value = "100", message = "Share percentage must be at most 100") @Digits(
              integer = 13,
              fraction = 4,
              message = "Share percentage must have at most 13 integer digits and 4 decimal places")
          BigDecimal sharePct) {}
}
