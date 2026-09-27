package com.buurman.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jooq.exception.IntegrityConstraintViolationException;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Property;
import com.buurman.domain.Unit;
import com.buurman.domain.UnitActiveTenancy;
import com.buurman.domain.UnitStatus;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.domain.identifier.UnitIdentifier;
import com.buurman.dto.request.BulkCreateUnitsRequest;
import com.buurman.dto.request.CreateUnitRequest;
import com.buurman.dto.request.UpdateUnitRequest;
import com.buurman.dto.response.UnitGridRowResponse;
import com.buurman.dto.response.UnitResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.mapper.UnitMapper;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.ExpenseAllocationRepository;
import com.buurman.repository.PropertyOccupancyPeriodRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UnitRepository;
import com.buurman.repository.WwsCalculationRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.FeatureFlags;
import com.buurman.util.SidGenerator;

import lombok.RequiredArgsConstructor;

/**
 * Manages {@link Unit}s within a {@link Property} and owns the two invariants that protect the unit
 * model:
 *
 * <ol>
 *   <li>a property always keeps at least one unit ({@link #deleteUnit} refuses to remove the last
 *       one, and refuses to remove a unit that still has an active contract);
 *   <li>the property's implicit stand-in unit is promoted in place — never deleted and recreated —
 *       the moment a second unit is added, so its contracts, payments, occupancy history, photos
 *       and WWS calculations stay attached to the same row.
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class UnitService {

  private final UnitRepository unitRepository;
  private final PropertyRepository propertyRepository;
  private final ContractRepository contractRepository;
  private final PropertyOccupancyPeriodRepository occupancyPeriodRepository;
  private final WwsCalculationRepository wwsCalculationRepository;
  private final ExpenseAllocationRepository expenseAllocationRepository;
  private final UnitMapper unitMapper;

  // Reserved for per-unit vacancy-day calculations once unit-level occupancy periods land
  // (BUUR-106 Task 12); listUnits() cannot derive vacancyDays without that work.
  private final Clock clock;

  // Gates createUnit/bulkCreateUnits only (BUUR-106). Several defects — WWS pricing reads,
  // occupancy reporting and expense-allocation edits — are still property-scoped rather than
  // unit-scoped, and only manifest once a property has more than one unit. createInitialUnit, the
  // V070 backfill, reads (getUnit/listUnits) and edits of existing units (updateUnit/deleteUnit)
  // are deliberately never gated: the never-zero-units invariant and read access must survive a
  // team being piloted and then switched back off.
  private final FeatureFlagService featureFlagService;

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public UnitResponse createUnit(
      PropertyIdentifier propertyIdentifier, CreateUnitRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    requireMultiUnitEnabled(teamId);
    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);

    promoteImplicitUnit(property.getId(), teamId, principal.getUserId());

    Unit unit = unitMapper.toEntity(request);
    unit.setIdentifier(Optional.of(SidGenerator.newUnitId()));
    unit.setTeamId(teamId);
    unit.setPropertyId(property.getId());
    unit.setCreatedBy(Optional.of(principal.getUserId()));
    unit.setUpdatedBy(Optional.of(principal.getUserId()));

    Unit saved = saveOrTranslateDuplicate(unit, request.unitNumber());
    return unitMapper.toResponse(saved, propertyIdentifier);
  }

  /**
   * Creates the first unit of a brand-new property, called from {@link
   * PropertyService#createProperty} inside the same transaction as the property insert, so the
   * never-zero-units invariant holds from the property's first moment. Deliberately skips {@link
   * #promoteImplicitUnit} — a property that was just created has no earlier implicit unit to
   * promote — and takes {@code implicit} as an explicit argument rather than deriving it, since
   * that decision ("did the caller supply unit details, or do we synthesize a default?") was
   * already made by the caller.
   */
  Unit createInitialUnit(
      UUID propertyId, CreateUnitRequest request, boolean implicit, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    Unit unit = unitMapper.toEntity(request);
    unit.setIdentifier(Optional.of(SidGenerator.newUnitId()));
    unit.setTeamId(teamId);
    unit.setPropertyId(propertyId);
    unit.setImplicit(implicit);
    unit.setCreatedBy(Optional.of(principal.getUserId()));
    unit.setUpdatedBy(Optional.of(principal.getUserId()));

    return saveOrTranslateDuplicate(unit, request.unitNumber());
  }

  /**
   * Splits a property into {@code request.count()} labeled units in one batch (e.g. "6 apartments
   * numbered 1-6"). If the property still has its implicit stand-in unit, that unit becomes #1 of
   * the batch — promoted in place, keeping its id/identifier so its contracts, payments, occupancy
   * history, photos and WWS calculations stay attached — and only the remaining {@code count - 1}
   * rows are newly inserted. All labels are validated against the property's existing unit numbers
   * before anything is written, and the whole batch is one {@code @Transactional} unit of work, so
   * a collision (or a same-property duplicate slipping past validation, translated below) leaves
   * zero units created rather than a partial batch.
   */
  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public List<UnitResponse> bulkCreateUnits(
      PropertyIdentifier propertyIdentifier,
      BulkCreateUnitsRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    requireMultiUnitEnabled(teamId);
    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);

    List<Unit> existingUnits =
        unitRepository.findAllByPropertyIdAndTeamId(property.getId(), teamId);
    Optional<Unit> implicitUnit = existingUnits.stream().filter(Unit::isImplicit).findFirst();

    List<String> labels = numberingLabels(request);
    rejectCollisionsWithExistingUnits(labels, existingUnits, implicitUnit);

    List<UnitResponse> created = new ArrayList<>();
    for (int i = 0; i < labels.size(); i++) {
      String label = labels.get(i);
      Unit unit =
          (i == 0 && implicitUnit.isPresent())
              ? promoteForBulkCreate(implicitUnit.get(), label, i, principal)
              : newBulkUnit(property.getId(), teamId, label, i, request, principal);
      Unit saved = saveOrTranslateDuplicate(unit, label);
      created.add(unitMapper.toResponse(saved, propertyIdentifier));
    }
    return List.copyOf(created);
  }

  /**
   * Checked up front so the batch fails atomically with a message naming the actual conflicting
   * number(s), instead of surfacing whichever row happens to hit {@code uq_units_property_number}
   * mid-loop. The implicit unit is excluded because this batch renumbers it away — its current
   * label is not a real conflict.
   */
  private void rejectCollisionsWithExistingUnits(
      List<String> labels, List<Unit> existingUnits, Optional<Unit> implicitUnit) {
    Set<String> occupiedNumbers =
        existingUnits.stream()
            .filter(
                unit ->
                    implicitUnit
                        .map(candidate -> !candidate.getId().equals(unit.getId()))
                        .orElse(true))
            .map(Unit::getUnitNumber)
            .collect(Collectors.toSet());
    List<String> collisions = labels.stream().filter(occupiedNumbers::contains).toList();
    if (!collisions.isEmpty()) {
      throw new BusinessRuleException(
          "Cannot bulk-create units: unit number "
              + String.join(", ", collisions)
              + " already exists on this property.");
    }
  }

  /**
   * Bulk-create's promotion counterpart to {@link #promoteImplicitUnit}: intentionally duplicated
   * rather than shared behind an optional-label/sortOrder parameter, because single-create
   * promotion must never renumber. Keep the two in sync manually — a field added to one promotion
   * path (e.g. clearing a stat that shouldn't survive promotion) likely belongs in the other too.
   */
  private Unit promoteForBulkCreate(
      Unit implicitUnit, String label, int sortOrder, UserPrincipal principal) {
    implicitUnit.setImplicit(false);
    implicitUnit.setUnitNumber(label);
    implicitUnit.setSortOrder(sortOrder);
    // V070 copies the building's whole area_value onto the implicit unit, and its
    // allocation_share (if any) likewise describes the whole building, not this one promoted unit
    // among its new siblings. Left in place, a split into N units would leave the entire
    // building's floor area on unit 1 — silently charging it every AREA-based expense in full
    // (BUUR-106 Important 4) and, separately, feeding that unit's WWS legal rent ceiling with the
    // wrong area. The new siblings created alongside it already start with neither set.
    implicitUnit.setAreaValue(Optional.empty());
    implicitUnit.setAllocationShare(Optional.empty());
    implicitUnit.setUpdatedBy(Optional.of(principal.getUserId()));
    return implicitUnit;
  }

  private Unit newBulkUnit(
      UUID propertyId,
      UUID teamId,
      String label,
      int sortOrder,
      BulkCreateUnitsRequest request,
      UserPrincipal principal) {
    return Unit.builder()
        .identifier(Optional.of(SidGenerator.newUnitId()))
        .teamId(teamId)
        .propertyId(propertyId)
        .unitNumber(label)
        .unitType(request.unitType())
        .status(UnitStatus.VACANT)
        .sortOrder(sortOrder)
        .floor(
            request.numberingPattern() == BulkCreateUnitsRequest.NumberingPattern.FLOOR_DOT_INDEX
                ? Optional.of(request.startFloor().orElse(0))
                : Optional.empty())
        .createdBy(Optional.of(principal.getUserId()))
        .updatedBy(Optional.of(principal.getUserId()))
        .build();
  }

  private List<String> numberingLabels(BulkCreateUnitsRequest request) {
    List<String> labels = new ArrayList<>();
    for (int i = 0; i < request.count(); i++) {
      switch (request.numberingPattern()) {
        case NUMERIC -> labels.add(String.valueOf(i + 1));
        case ALPHABETIC -> labels.add(alphabeticLabel(i));
        // "%02d" is zero-padded to two digits only up to index 99; @Max(200) allows counts that
        // widen it to three digits (e.g. "1.100"). Labels stay unique either way, so this is
        // graceful degradation, not a bug — just not literally "two-digit" past that point.
        case FLOOR_DOT_INDEX ->
            labels.add(request.startFloor().orElse(0) + "." + String.format("%02d", i + 1));
      }
    }
    return labels;
  }

  /**
   * Spreadsheet-column style: {@code 0 -> "A"}, {@code 25 -> "Z"}, {@code 26 -> "AA"}, so a count
   * above 26 still produces unique labels.
   */
  private String alphabeticLabel(int index) {
    StringBuilder label = new StringBuilder();
    int remaining = index;
    while (remaining >= 0) {
      label.insert(0, (char) ('A' + (remaining % 26)));
      remaining = (remaining / 26) - 1;
    }
    return label.toString();
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public UnitResponse updateUnit(
      UnitIdentifier identifier, UpdateUnitRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Unit unit = unitRepository.getByIdentifierAndTeamId(identifier, teamId);

    unitMapper.updateEntity(unit, request);
    unit.setUpdatedBy(Optional.of(principal.getUserId()));

    Unit saved = saveOrTranslateDuplicate(unit, request.unitNumber());
    Property property = propertyRepository.getByIdAndTeamId(saved.getPropertyId(), teamId);
    return unitMapper.toResponse(saved, property.getIdentifier().orElseThrow());
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public void deleteUnit(UnitIdentifier identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Unit unit = unitRepository.getByIdentifierAndTeamId(identifier, teamId);

    if (unitRepository.countActiveByPropertyIdAndTeamId(unit.getPropertyId(), teamId) <= 1) {
      throw new BusinessRuleException(
          "Cannot delete the last unit of a property. A property must always have at least one"
              + " unit.");
    }
    if (contractRepository.countActiveByUnitId(unit.getId(), teamId) > 0) {
      throw new BusinessRuleException(
          "Cannot delete a unit with an active contract. End the contract first.");
    }
    // A soft-deleted unit is invisible to findByIdsAndTeamId/getByIdAndTeamId lookups, so any
    // dependent that resolves its unit by id at read time (contract history, occupancy periods, WWS
    // calculations, expense allocations) would start throwing instead of rendering. Refusing here —
    // like the last-unit and active-contract guards above — is kinder than three endpoints
    // degrading into 404s and 500s.
    if (contractRepository.existsByUnitId(unit.getId(), teamId)) {
      throw new BusinessRuleException(
          "Cannot delete a unit with contract history. Its past contracts must remain readable.");
    }
    if (occupancyPeriodRepository.existsByUnitIdAndTeamId(unit.getId(), teamId)) {
      throw new BusinessRuleException(
          "Cannot delete a unit with occupancy period history recorded against it.");
    }
    if (wwsCalculationRepository.existsByUnitIdAndTeamId(unit.getId(), teamId)) {
      throw new BusinessRuleException(
          "Cannot delete a unit with WWS calculations recorded against it.");
    }
    if (expenseAllocationRepository.existsByUnitIdAndTeamId(unit.getId(), teamId)) {
      throw new BusinessRuleException(
          "Cannot delete a unit with expense allocations recorded against it.");
    }
    unitRepository.softDelete(unit.getId(), teamId, principal.getUserId());
  }

  public UnitResponse getUnit(UnitIdentifier identifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Unit unit = unitRepository.getByIdentifierAndTeamId(identifier, teamId);
    Property property = propertyRepository.getByIdAndTeamId(unit.getPropertyId(), teamId);
    return unitMapper.toResponse(unit, property.getIdentifier().orElseThrow());
  }

  public List<UnitGridRowResponse> listUnits(
      PropertyIdentifier propertyIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);
    List<Unit> units = unitRepository.findAllByPropertyIdAndTeamId(property.getId(), teamId);

    List<UUID> unitIds = units.stream().map(Unit::getId).toList();
    // The schema permits two ACTIVE contracts on one unit (contracts.unit_id has only a plain
    // index, no uniqueness constraint), and findActiveTenanciesByUnitIds orders by start date
    // descending, so keeping the first row per unit picks the most recently started tenancy
    // instead of throwing on a duplicate key.
    Map<UUID, UnitActiveTenancy> tenancyByUnitId =
        contractRepository.findActiveTenanciesByUnitIds(unitIds, teamId).stream()
            .collect(
                Collectors.toMap(
                    UnitActiveTenancy::unitId, Function.identity(), (first, second) -> first));

    return units.stream().map(unit -> toGridRow(unit, tenancyByUnitId.get(unit.getId()))).toList();
  }

  /**
   * Flips the property's implicit stand-in unit into a real one so it keeps its contracts, photos,
   * occupancy history and WWS calculations when the landlord starts tracking units explicitly.
   * Relies on the {@code uq_units_one_implicit_per_property} unique index to guarantee at most one
   * live implicit unit per property, so the unit to promote is unambiguous. Promotes in place —
   * never delete-and-recreate.
   *
   * <p>See {@link #promoteForBulkCreate} for bulk-create's counterpart, deliberately kept separate
   * (single-create must not renumber) — keep both in sync manually.
   */
  private void promoteImplicitUnit(UUID propertyId, UUID teamId, UUID actorId) {
    unitRepository.findAllByPropertyIdAndTeamId(propertyId, teamId).stream()
        .filter(Unit::isImplicit)
        .findFirst()
        .ifPresent(
            implicitUnit -> {
              implicitUnit.setImplicit(false);
              implicitUnit.setUpdatedBy(Optional.of(actorId));
              unitRepository.save(implicitUnit);
            });
  }

  /**
   * Blast-radius control for the defects that only manifest on a multi-unit property (BUUR-106):
   * WWS pricing reads, occupancy reporting and expense-allocation edits are still property-scoped.
   * Refusing here keeps every property's implicit unit and single-unit landlords unaffected, and
   * confines the exposure to teams a pilot opts in via a per-team {@link FeatureFlags#MULTI_UNIT}
   * override. Deliberately not applied to {@link #createInitialUnit} or reads/edits of units that
   * already exist.
   */
  private void requireMultiUnitEnabled(UUID teamId) {
    if (!featureFlagService.isEnabled(FeatureFlags.MULTI_UNIT, teamId)) {
      throw new BusinessRuleException(
          "Multiple units per property is not enabled for your team yet.");
    }
  }

  /**
   * {@code units} has more than one CHECK/unique constraint that jOOQ reports through the same
   * {@link IntegrityConstraintViolationException} type -- notably {@code
   * chk_units_allocation_share} (0-100) alongside {@code uq_units_property_number}. Rewriting every
   * such exception as "already exists" would misreport an out-of-range allocationShare as a
   * duplicate unit number (BUUR-106 follow-up register, section F, item 9), so only the specific
   * constraint this method is actually translating is caught here; anything else propagates
   * unchanged.
   */
  private Unit saveOrTranslateDuplicate(Unit unit, String unitNumber) {
    try {
      return unitRepository.save(unit);
    } catch (IntegrityConstraintViolationException e) {
      if (e.getMessage() != null && e.getMessage().contains("uq_units_property_number")) {
        throw new BusinessRuleException(
            "A unit numbered " + unitNumber + " already exists on this property.");
      }
      throw e;
    }
  }

  private UnitGridRowResponse toGridRow(Unit unit, @Nullable UnitActiveTenancy tenancy) {
    Optional<UnitActiveTenancy> activeTenancy = Optional.ofNullable(tenancy);
    return new UnitGridRowResponse(
        unit.getIdentifier().orElseThrow(),
        unit.getUnitNumber(),
        unit.getName(),
        unit.getUnitType(),
        unit.getStatus(),
        activeTenancy.map(UnitActiveTenancy::tenantName),
        activeTenancy.map(UnitActiveTenancy::rentAmount),
        activeTenancy.map(UnitActiveTenancy::rentAmountCurrency),
        // Vacancy days require per-unit occupancy-period tracking, not yet implemented
        // (BUUR-106 Task 12) — left empty rather than guessed.
        Optional.empty());
  }
}
