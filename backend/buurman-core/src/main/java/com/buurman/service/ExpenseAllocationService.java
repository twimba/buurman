package com.buurman.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import com.buurman.domain.AllocationBasis;
import com.buurman.domain.ExpenseAllocation;
import com.buurman.domain.Unit;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UnitRepository;
import com.buurman.util.CurrencyUtils;
import com.buurman.util.MoneyAmount;

/**
 * Splits a building-level expense across a property's units.
 *
 * <p>{@link #computeAllocations(MoneyAmount, AllocationBasis, List)} is pure and I/O-free by design
 * so it can be unit-tested directly, independent of persistence. Persisting the result and wiring
 * this service into the rest of the application (via {@code allocate} / {@code recompute} methods
 * backed by an {@code ExpenseAllocationRepository}) is deliberately out of scope for this task.
 *
 * <p><strong>Deviation from the target constructor:</strong> the eventual constructor is meant to
 * be {@code (ExpenseAllocationRepository, UnitRepository, PropertyRepository, Clock)}, fixed at
 * four dependencies so a later persistence task does not need to change call sites. {@code
 * ExpenseAllocationRepository} does not exist yet, so this constructor is temporarily {@code
 * (UnitRepository, PropertyRepository, Clock)} — the three dependencies that do exist. The
 * repository parameter should be added back, in the first position, once it is introduced.
 */
@Service
public class ExpenseAllocationService {

  private final @Nullable UnitRepository unitRepository;
  private final @Nullable PropertyRepository propertyRepository;
  private final @Nullable Clock clock;

  /**
   * The dependencies are nullable only so tests can exercise the pure {@link #computeAllocations}
   * method without a Spring context (see the class-level Javadoc for why the arity is fixed).
   * Production code always receives real, non-null instances via Spring's constructor injection.
   */
  public ExpenseAllocationService(
      @Nullable UnitRepository unitRepository,
      @Nullable PropertyRepository propertyRepository,
      @Nullable Clock clock) {
    this.unitRepository = unitRepository;
    this.propertyRepository = propertyRepository;
    this.clock = clock;
  }

  /**
   * Splits {@code total} across {@code units}. Pure and I/O-free so it can be unit-tested directly.
   *
   * <p>Arithmetic happens in minor units and the remainder is distributed by the largest-remainder
   * method, so the returned amounts always sum to exactly {@code total} — no drifting cent. Works
   * for negative totals (credit notes) because the leftover is signed and handed out in the same
   * direction as the leftover itself.
   */
  public List<ExpenseAllocation> computeAllocations(
      MoneyAmount total, AllocationBasis basis, List<Unit> units) {
    if (units.isEmpty()) {
      return List.of();
    }

    AllocationBasis effectiveBasis = resolveBasis(basis, units);
    List<BigDecimal> weights = weightsFor(effectiveBasis, units);
    BigDecimal weightTotal = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

    long totalMinor = total.toMinorUnits();
    long[] amounts = new long[units.size()];
    BigDecimal[] remainders = new BigDecimal[units.size()];
    long distributed = 0;

    for (int i = 0; i < units.size(); i++) {
      BigDecimal exact =
          weightTotal.signum() == 0
              ? BigDecimal.ZERO
              : BigDecimal.valueOf(totalMinor)
                  .multiply(weights.get(i))
                  .divide(weightTotal, 10, RoundingMode.HALF_UP);
      amounts[i] = exact.setScale(0, RoundingMode.DOWN).longValueExact();
      remainders[i] = exact.subtract(BigDecimal.valueOf(amounts[i])).abs();
      distributed += amounts[i];
    }

    long leftover = totalMinor - distributed;
    long step = leftover >= 0 ? 1 : -1;
    List<Integer> order =
        IntStream.range(0, units.size())
            .boxed()
            .sorted((a, b) -> remainders[b].compareTo(remainders[a]))
            .toList();

    int cursor = 0;
    while (leftover != 0 && !order.isEmpty()) {
      amounts[order.get(cursor % order.size())] += step;
      leftover -= step;
      cursor++;
    }

    List<ExpenseAllocation> allocations = new ArrayList<>();
    for (int i = 0; i < units.size(); i++) {
      allocations.add(
          ExpenseAllocation.builder()
              .unitId(units.get(i).getId())
              .teamId(units.get(i).getTeamId())
              .amount(minorToMoney(amounts[i], total.currency()))
              .basis(effectiveBasis)
              .build());
    }
    return List.copyOf(allocations);
  }

  /**
   * AREA is only meaningful when at least one unit has an area, and CUSTOM only when at least one
   * unit has an allocation share; otherwise EQUAL is the honest description of what actually
   * happened, and is what gets recorded on the returned rows.
   */
  private AllocationBasis resolveBasis(AllocationBasis requested, List<Unit> units) {
    if (requested == AllocationBasis.AREA
        && units.stream().allMatch(u -> u.getAreaValue().isEmpty())) {
      return AllocationBasis.EQUAL;
    }
    if (requested == AllocationBasis.CUSTOM
        && units.stream().allMatch(u -> u.getAllocationShare().isEmpty())) {
      return AllocationBasis.EQUAL;
    }
    return requested;
  }

  private List<BigDecimal> weightsFor(AllocationBasis basis, List<Unit> units) {
    return units.stream()
        .map(
            unit ->
                switch (basis) {
                  case AREA -> unit.getAreaValue().orElse(BigDecimal.ZERO);
                  case CUSTOM -> unit.getAllocationShare().orElse(BigDecimal.ZERO);
                  case EQUAL, MANUAL -> BigDecimal.ONE;
                })
        .toList();
  }

  private MoneyAmount minorToMoney(long minor, String currency) {
    int digits = CurrencyUtils.getFractionalDigits(currency);
    return new MoneyAmount(BigDecimal.valueOf(minor, digits), currency);
  }
}
