package com.buurman.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import com.buurman.domain.identifier.UnitIdentifier;
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
   * responsible for only calling this when the expense's {@code unitId} is empty, and for only
   * calling it when an allocation-relevant field actually changed). Uses the property's current
   * allocation basis.
   *
   * <p>Refuses to overwrite an active MANUAL set — a landlord's hand-split survives every unrelated
   * edit to the expense. Lifting a MANUAL override is only ever done explicitly, via {@link
   * #overrideManual} again or the recompute endpoint ({@link #recompute}).
   */
  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public List<ExpenseAllocationResponse> allocate(Expense expense, UUID actorId) {
    if (hasActiveManualBasis(expense)) {
      return getAllocations(expense);
    }
    return computeAndPersist(expense, actorId);
  }

  /**
   * Explicit, user-triggered re-run of the automatic split using the property's <em>current</em>
   * allocation basis. Does not run implicitly on a basis change — see the class Javadoc for why.
   * Unlike {@link #allocate}, this overwrites an active MANUAL set too — recompute is the one
   * auditable, opt-in act that is allowed to lift a hand-split override.
   */
  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public List<ExpenseAllocationResponse> recompute(Expense expense, UUID actorId) {
    return computeAndPersist(expense, actorId);
  }

  /**
   * Retires an expense's active allocation rows without replacing them, for when a building-level
   * expense is edited to belong to a single unit. Without this, the previous per-unit rows stay
   * active alongside the now-direct unit charge, double-counting the expense on a service-charge
   * settlement statement.
   */
  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public void retireAllocations(Expense expense, UUID actorId) {
    requireExpenseAllocationRepository()
        .replaceForExpense(expense.getId(), expense.getTeamId(), List.of(), actorId);
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

    // Caught here, up front, rather than left to trip uq_expense_allocations_expense_unit: a
    // duplicate unitIdentifier would otherwise surface as an opaque 409 from the unique index,
    // naming no unit and giving the caller nothing to fix.
    Set<UnitIdentifier> seenUnitIdentifiers = new HashSet<>();
    for (ManualAllocationEntry entry : request.entries()) {
      if (!seenUnitIdentifiers.add(entry.unitIdentifier())) {
        throw new BusinessRuleException(
            "Duplicate allocation entry for unit " + entry.unitIdentifier().value() + ".");
      }
    }

    String currency = expense.getAmount().currency();
    // Normalise each entry to the currency's scale BEFORE summing: validating the raw BigDecimals
    // and only rounding afterwards (via MoneyAmount.of below) lets sub-minor-unit entries such as
    // 0.005/0.005/9.99 pass the check on paper while rounding to 0.01/0.01/9.99 = 10.01 once
    // persisted — silently breaking the one guarantee MANUAL allocations advertise: that the split
    // sums to exactly the expense's total.
    List<MoneyAmount> normalizedEntries =
        request.entries().stream().map(entry -> MoneyAmount.of(entry.amount(), currency)).toList();
    BigDecimal supplied =
        normalizedEntries.stream().map(MoneyAmount::value).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (supplied.compareTo(expense.getAmount().value()) != 0) {
      throw new BusinessRuleException(
          "Manual allocations total "
              + supplied.toPlainString()
              + " but the expense is "
              + expense.getAmount().value().toPlainString()
              + ".");
    }

    List<ExpenseAllocation> allocations = new ArrayList<>();
    for (int i = 0; i < request.entries().size(); i++) {
      ManualAllocationEntry entry = request.entries().get(i);
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
              .amount(normalizedEntries.get(i))
              .basis(AllocationBasis.MANUAL)
              .build());
    }

    requireExpenseAllocationRepository()
        .replaceForExpense(expense.getId(), expense.getTeamId(), allocations, actorId);

    return getAllocations(expense);
  }

  /**
   * Team-scoped read of an expense's current active allocation rows. Also resolves the property's
   * <em>current</em> allocation basis to populate each row's {@code requestedBasis} and any
   * fallback {@code warnings} — see {@link ExpenseAllocationResponse}.
   */
  public List<ExpenseAllocationResponse> getAllocations(Expense expense) {
    List<ExpenseAllocation> allocations =
        requireExpenseAllocationRepository()
            .findByExpenseIdAndTeamId(expense.getId(), expense.getTeamId());
    if (allocations.isEmpty()) {
      return List.of();
    }

    AllocationBasis requestedBasis =
        requirePropertyRepository()
            .getByIdAndTeamId(expense.getPropertyId(), expense.getTeamId())
            .getAllocationBasis();
    return toResponses(allocations, expense.getTeamId(), requestedBasis);
  }

  private List<ExpenseAllocationResponse> toResponses(
      List<ExpenseAllocation> allocations, UUID teamId, AllocationBasis requestedBasis) {
    if (allocations.isEmpty()) {
      return List.of();
    }

    List<UUID> unitIds = allocations.stream().map(ExpenseAllocation::getUnitId).distinct().toList();
    Map<UUID, Unit> unitsById =
        requireUnitRepository().findByIdsAndTeamId(unitIds, teamId).stream()
            .collect(Collectors.toMap(Unit::getId, unit -> unit));

    List<String> warnings = warningsFor(requestedBasis, unitsById.values());

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
                  requestedBasis,
                  warnings,
                  allocation.getCreatedAt().orElseThrow(),
                  allocation.getUpdatedAt());
            })
        .toList();
  }

  /**
   * Non-fatal warnings naming which unit(s) are missing the weight that {@code requestedBasis}
   * needs, i.e. which unit(s) would force (or did force) a silent fallback to EQUAL. Empty for
   * EQUAL/MANUAL, since neither basis has a "missing weight" concept.
   */
  private List<String> warningsFor(AllocationBasis requestedBasis, Collection<Unit> units) {
    if (requestedBasis != AllocationBasis.AREA && requestedBasis != AllocationBasis.CUSTOM) {
      return List.of();
    }
    String weightName = requestedBasis == AllocationBasis.AREA ? "area" : "allocation share";
    return units.stream()
        .filter(unit -> weightIsAbsent(requestedBasis, unit))
        .map(Unit::getUnitNumber)
        .sorted()
        .map(
            unitNumber ->
                "Unit "
                    + unitNumber
                    + " has no "
                    + weightName
                    + " set; the split fell back to EQUAL.")
        .toList();
  }

  /**
   * Whether the expense's currently active allocation rows were a MANUAL override. A MANUAL set is
   * always written and retired as one unit ({@link #overrideManual} replaces the whole active set
   * in one call), so checking the first row is sufficient.
   */
  private boolean hasActiveManualBasis(Expense expense) {
    return requireExpenseAllocationRepository()
        .findByExpenseIdAndTeamId(expense.getId(), expense.getTeamId())
        .stream()
        .anyMatch(allocation -> allocation.getBasis() == AllocationBasis.MANUAL);
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
   * AREA is only meaningful when every active unit actually carries an area, and CUSTOM only when
   * every active unit carries a share; otherwise EQUAL is the honest description of what actually
   * happened, and is what gets recorded on the returned rows.
   *
   * <p>Falls back in two distinct cases:
   *
   * <ul>
   *   <li><b>Any unit is missing the weight entirely</b> (absent, e.g. {@code area_value} was never
   *       set — the default state right after a property is split into units, since bulk-created
   *       units get no area). Without this, the weightless units are silently charged exactly
   *       {@code 0.00} while the others absorb their share, still stamped with the requested basis
   *       as if every unit's weight had actually been considered.
   *   <li><b>Every unit's weight is present but sums to zero</b> (e.g. every unit's CUSTOM share is
   *       explicitly {@code 0}) — a zero weight total would otherwise drive the largest-remainder
   *       loop below with nothing to distribute by.
   * </ul>
   *
   * An explicit {@code 0} on some (not all) units is a landlord's decision — that unit is charged
   * nothing and the requested basis stands, splitting the total across the remaining units.
   */
  private AllocationBasis resolveBasis(AllocationBasis requested, List<Unit> units) {
    if (requested != AllocationBasis.AREA && requested != AllocationBasis.CUSTOM) {
      return requested;
    }
    boolean anyUnitMissingWeight = units.stream().anyMatch(unit -> weightIsAbsent(requested, unit));
    if (anyUnitMissingWeight) {
      return AllocationBasis.EQUAL;
    }
    BigDecimal weightTotal =
        weightsFor(requested, units).stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    if (weightTotal.signum() == 0) {
      return AllocationBasis.EQUAL;
    }
    return requested;
  }

  /**
   * Whether {@code unit} has no value at all for the weight {@code basis} needs (AREA or CUSTOM).
   */
  private boolean weightIsAbsent(AllocationBasis basis, Unit unit) {
    return switch (basis) {
      case AREA -> unit.getAreaValue().isEmpty();
      case CUSTOM -> unit.getAllocationShare().isEmpty();
      case EQUAL, MANUAL -> false;
    };
  }

  private List<BigDecimal> weightsFor(AllocationBasis basis, List<Unit> units) {
    return units.stream()
        .map(
            unit ->
                switch (basis) {
                  case AREA -> unit.getAreaValue().orElse(BigDecimal.ZERO);
                  case CUSTOM -> unit.getAllocationShare().orElse(BigDecimal.ZERO);
                  case EQUAL -> BigDecimal.ONE;
                  // MANUAL is a per-expense override (see overrideManual), never a property-level
                  // basis — PropertyService.updateAllocation rejects it before this is ever
                  // reached, so getting here means that guard was bypassed. Silently treating it
                  // as EQUAL would produce rows stamped MANUAL over a split nobody actually chose.
                  case MANUAL ->
                      throw new IllegalStateException(
                          "MANUAL is a per-expense override and cannot be used as a property's"
                              + " allocation basis.");
                })
        .toList();
  }

  private MoneyAmount minorToMoney(long minor, String currency) {
    int digits = CurrencyUtils.getFractionalDigits(currency);
    return new MoneyAmount(BigDecimal.valueOf(minor, digits), currency);
  }
}
