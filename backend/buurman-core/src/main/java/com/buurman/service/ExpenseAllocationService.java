package com.buurman.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.AllocationBasis;
import com.buurman.domain.Expense;
import com.buurman.domain.ExpenseAllocation;
import com.buurman.domain.Property;
import com.buurman.domain.Unit;
import com.buurman.dto.request.ManualAllocationRequest;
import com.buurman.dto.request.ManualAllocationRequest.ManualAllocationEntry;
import com.buurman.dto.response.ExpenseAllocationResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ExpenseAllocationRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UnitRepository;
import com.buurman.util.CurrencyUtils;
import com.buurman.util.MoneyAmount;

/**
 * Splits a building-level expense across a property's units, and persists the result.
 *
 * <p>{@link #computeAllocations(MoneyAmount, AllocationBasis, List)} is pure and I/O-free by design
 * so it can be unit-tested directly, independent of persistence. Everything else in this class
 * wires that pure computation to {@link ExpenseAllocationRepository#replaceForExpense}, which
 * retires the previous set of rows (soft delete) before inserting the new one so a re-allocation
 * never trips {@code uq_expense_allocations_expense_unit}.
 *
 * <p>{@link #allocate} runs automatically whenever a building-level expense (no {@code unitId}) is
 * created or updated, using the property's <em>current</em> allocation basis. {@link #recompute} is
 * the same computation triggered explicitly by the caller — changing a property's basis does not
 * retroactively rewrite history, because NL service-charge settlement statements built from these
 * rows are legal documents; recomputation must be an auditable, opt-in act. {@link #overrideManual}
 * instead takes the caller's own per-unit amounts (MANUAL basis) after validating they sum to
 * exactly the expense's total.
 */
@Service
public class ExpenseAllocationService {

  private final @Nullable ExpenseAllocationRepository expenseAllocationRepository;
  private final @Nullable UnitRepository unitRepository;
  private final @Nullable PropertyRepository propertyRepository;
  private final @Nullable Clock clock;

  /**
   * The dependencies are nullable only so tests can exercise the pure {@link #computeAllocations}
   * method without a Spring context. Production code always receives real, non-null instances via
   * Spring's constructor injection.
   */
  public ExpenseAllocationService(
      @Nullable ExpenseAllocationRepository expenseAllocationRepository,
      @Nullable UnitRepository unitRepository,
      @Nullable PropertyRepository propertyRepository,
      @Nullable Clock clock) {
    this.expenseAllocationRepository = expenseAllocationRepository;
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
   * Runs automatically whenever a building-level expense is created or updated (the caller is
   * responsible for only calling this when the expense's {@code unitId} is empty). Uses the
   * property's current allocation basis.
   */
  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public List<ExpenseAllocationResponse> allocate(Expense expense, UUID actorId) {
    return computeAndPersist(expense, actorId);
  }

  /**
   * Explicit, user-triggered re-run of the automatic split using the property's <em>current</em>
   * allocation basis. Does not run implicitly on a basis change — see the class Javadoc for why.
   */
  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public List<ExpenseAllocationResponse> recompute(Expense expense, UUID actorId) {
    return computeAndPersist(expense, actorId);
  }

  private List<ExpenseAllocationResponse> computeAndPersist(Expense expense, UUID actorId) {
    if (expense.getUnitId().isPresent()) {
      throw new BusinessRuleException(
          "Expense belongs to a single unit and has no building-level allocation to compute.");
    }

    Property property =
        requirePropertyRepository().getByIdAndTeamId(expense.getPropertyId(), expense.getTeamId());
    List<Unit> units =
        requireUnitRepository().findAllByPropertyIdAndTeamId(property.getId(), expense.getTeamId());
    List<ExpenseAllocation> allocations =
        computeAllocations(expense.getAmount(), property.getAllocationBasis(), units);

    requireExpenseAllocationRepository()
        .replaceForExpense(expense.getId(), expense.getTeamId(), allocations, actorId);

    return getAllocations(expense);
  }

  /**
   * Caller-supplied per-unit override (MANUAL basis). Rejects the request with a {@link
   * BusinessRuleException} (409) unless the entries sum to exactly the expense's total.
   */
  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public List<ExpenseAllocationResponse> overrideManual(
      Expense expense, ManualAllocationRequest request, UUID actorId) {
    if (expense.getUnitId().isPresent()) {
      throw new BusinessRuleException(
          "Expense belongs to a single unit and cannot be allocated across units.");
    }

    BigDecimal supplied =
        request.entries().stream()
            .map(ManualAllocationEntry::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (supplied.compareTo(expense.getAmount().value()) != 0) {
      throw new BusinessRuleException(
          "Manual allocations total "
              + supplied.toPlainString()
              + " but the expense is "
              + expense.getAmount().value().toPlainString()
              + ".");
    }

    List<ExpenseAllocation> allocations = new ArrayList<>();
    for (ManualAllocationEntry entry : request.entries()) {
      Unit unit =
          requireUnitRepository()
              .getByIdentifierAndTeamId(entry.unitIdentifier(), expense.getTeamId());
      if (!unit.getPropertyId().equals(expense.getPropertyId())) {
        throw new BusinessRuleException(
            "Unit "
                + entry.unitIdentifier().value()
                + " does not belong to this expense's property.");
      }
      allocations.add(
          ExpenseAllocation.builder()
              .unitId(unit.getId())
              .teamId(expense.getTeamId())
              .amount(MoneyAmount.of(entry.amount(), expense.getAmount().currency()))
              .basis(AllocationBasis.MANUAL)
              .build());
    }

    requireExpenseAllocationRepository()
        .replaceForExpense(expense.getId(), expense.getTeamId(), allocations, actorId);

    return getAllocations(expense);
  }

  /** Team-scoped read of an expense's current active allocation rows. */
  public List<ExpenseAllocationResponse> getAllocations(Expense expense) {
    List<ExpenseAllocation> allocations =
        requireExpenseAllocationRepository()
            .findByExpenseIdAndTeamId(expense.getId(), expense.getTeamId());
    return toResponses(allocations, expense.getTeamId());
  }

  private List<ExpenseAllocationResponse> toResponses(
      List<ExpenseAllocation> allocations, UUID teamId) {
    if (allocations.isEmpty()) {
      return List.of();
    }

    List<UUID> unitIds = allocations.stream().map(ExpenseAllocation::getUnitId).distinct().toList();
    Map<UUID, Unit> unitsById =
        requireUnitRepository().findByIdsAndTeamId(unitIds, teamId).stream()
            .collect(Collectors.toMap(Unit::getId, unit -> unit));

    return allocations.stream()
        .map(
            allocation -> {
              Unit unit =
                  Optional.ofNullable(unitsById.get(allocation.getUnitId()))
                      .orElseThrow(
                          () ->
                              new IllegalStateException(
                                  "Expense allocation "
                                      + allocation.getIdentifier().orElseThrow()
                                      + " references a unit that no longer exists"));
              return new ExpenseAllocationResponse(
                  allocation.getIdentifier().orElseThrow(),
                  unit.getIdentifier().orElseThrow(),
                  unit.getUnitNumber(),
                  Optional.of(allocation.getAmount().value()),
                  Optional.of(allocation.getAmount().currency()),
                  allocation.getBasis(),
                  allocation.getCreatedAt().orElseThrow(),
                  allocation.getUpdatedAt());
            })
        .toList();
  }

  private ExpenseAllocationRepository requireExpenseAllocationRepository() {
    return Objects.requireNonNull(
        expenseAllocationRepository, "ExpenseAllocationRepository is required for this operation");
  }

  private UnitRepository requireUnitRepository() {
    return Objects.requireNonNull(unitRepository, "UnitRepository is required for this operation");
  }

  private PropertyRepository requirePropertyRepository() {
    return Objects.requireNonNull(
        propertyRepository, "PropertyRepository is required for this operation");
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
