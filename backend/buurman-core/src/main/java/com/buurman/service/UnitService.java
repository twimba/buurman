package com.buurman.service;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.domain.identifier.UnitIdentifier;
import com.buurman.dto.request.CreateUnitRequest;
import com.buurman.dto.request.UpdateUnitRequest;
import com.buurman.dto.response.UnitGridRowResponse;
import com.buurman.dto.response.UnitResponse;
import com.buurman.exception.BusinessRuleException;
import com.buurman.mapper.UnitMapper;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UnitRepository;
import com.buurman.security.UserPrincipal;
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
  private final UnitMapper unitMapper;

  // Reserved for per-unit vacancy-day calculations once unit-level occupancy periods land
  // (BUUR-106 Task 12); listUnits() cannot derive vacancyDays without that work.
  private final Clock clock;

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public UnitResponse createUnit(
      PropertyIdentifier propertyIdentifier, CreateUnitRequest request, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
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

  private Unit saveOrTranslateDuplicate(Unit unit, String unitNumber) {
    try {
      return unitRepository.save(unit);
    } catch (IntegrityConstraintViolationException e) {
      throw new BusinessRuleException(
          "A unit numbered " + unitNumber + " already exists on this property.");
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
