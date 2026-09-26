package com.buurman.service;

import static com.buurman.util.SidGenerator.newOccupancyPeriodId;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.Property;
import com.buurman.domain.PropertyAcquisition;
import com.buurman.domain.PropertyFinancing;
import com.buurman.domain.PropertyOccupancyPeriod;
import com.buurman.domain.Sid;
import com.buurman.domain.Unit;
import com.buurman.domain.identifier.OccupancyPeriodIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.request.CreateOccupancyPeriodRequest;
import com.buurman.dto.request.EndOccupancyPeriodRequest;
import com.buurman.dto.request.UpdateOccupancyPeriodRequest;
import com.buurman.dto.response.OccupancyPeriodResponse;
import com.buurman.dto.response.PropertyTimelineResponse;
import com.buurman.dto.response.PropertyTimelineResponse.TimelineEntry;
import com.buurman.dto.response.PropertyTimelineResponse.TimelineEntryType;
import com.buurman.exception.BadRequestException;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyAcquisitionRepository;
import com.buurman.repository.PropertyFinancingRepository;
import com.buurman.repository.PropertyOccupancyPeriodRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UnitRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class OccupancyPeriodService {

  private final PropertyOccupancyPeriodRepository repository;
  private final PropertyRepository propertyRepository;
  private final UnitRepository unitRepository;
  private final ContractRepository contractRepository;
  private final ContractExtensionRepository contractExtensionRepository;
  private final PropertyAcquisitionRepository acquisitionRepository;
  private final PropertyFinancingRepository financingRepository;
  private final Clock clock;

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public OccupancyPeriodResponse create(
      PropertyIdentifier propertyIdentifier,
      CreateOccupancyPeriodRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);
    Unit unit = resolveUnit(property, request.unitIdentifier(), principal);

    // Validate no overlapping occupancy periods on this unit. Scoped by unit_id to match the
    // excl_occupancy_periods_no_overlap exclusion constraint (re-scoped from property_id to
    // unit_id in V068): two units of the same property may legitimately overlap.
    LocalDate endDate = request.endDate().orElse(LocalDate.of(9999, 12, 31));
    List<PropertyOccupancyPeriod> overlapping =
        repository.findOverlapping(unit.getId(), teamId, request.startDate(), endDate, null);
    if (!overlapping.isEmpty()) {
      throw new BusinessRuleException(
          "Cannot create self-occupancy period: overlaps with an existing period");
    }

    // Validate no overlapping contracts on this unit
    validateNoOverlappingContracts(unit.getId(), teamId, request.startDate(), endDate, null);

    Instant now = Instant.now(clock);
    PropertyOccupancyPeriod period =
        PropertyOccupancyPeriod.builder()
            .identifier(Optional.of(newOccupancyPeriodId()))
            .teamId(teamId)
            .propertyId(property.getId())
            .unitId(unit.getId())
            .startDate(request.startDate())
            .endDate(request.endDate())
            .type(request.type())
            .occupantName(request.occupantName())
            .monthlyImputedRent(request.monthlyImputedRent())
            .notes(request.notes())
            .createdAt(now)
            .updatedAt(now)
            .createdBy(principal.getUserId())
            .updatedBy(principal.getUserId())
            .build();

    repository.save(period);

    // TODO(BUUR-106 Task 12): occupancy status moved from `properties` to `units` in V068.
    // Marking the unit SELF_OCCUPIED when a period starts today or earlier needs a unit join.
    if (!request.startDate().isAfter(LocalDate.now(clock))) {
      log.warn(
          "Skipping automatic unit status update to SELF_OCCUPIED for property {}: status now"
              + " lives on units, not properties (BUUR-106 Task 12)",
          propertyIdentifier);
    }

    log.info(
        "Created self-occupancy period {} for property {}",
        period.getIdentifier().orElseThrow(),
        propertyIdentifier);

    return toResponse(period, propertyIdentifier, unit.getIdentifier().orElseThrow());
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public OccupancyPeriodResponse update(
      PropertyIdentifier propertyIdentifier,
      OccupancyPeriodIdentifier periodIdentifier,
      UpdateOccupancyPeriodRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);
    PropertyOccupancyPeriod period = repository.getByIdentifierAndTeamId(periodIdentifier, teamId);

    request.startDate().ifPresent(period::setStartDate);
    request.type().ifPresent(period::setType);
    request.endDate().ifPresent(d -> period.setEndDate(Optional.of(d)));
    request.occupantName().ifPresent(n -> period.setOccupantName(Optional.of(n)));
    request.monthlyImputedRent().ifPresent(r -> period.setMonthlyImputedRent(Optional.of(r)));
    request.notes().ifPresent(n -> period.setNotes(Optional.of(n)));

    // Validate no overlapping periods on this unit (excluding self). The unit a period belongs to
    // is fixed at creation, so this stays scoped to period.getUnitId() rather than the property.
    LocalDate endDate = period.getEndDate().orElse(LocalDate.of(9999, 12, 31));
    List<PropertyOccupancyPeriod> overlapping =
        repository.findOverlapping(
            period.getUnitId(), teamId, period.getStartDate(), endDate, period.getId());
    if (!overlapping.isEmpty()) {
      throw new BusinessRuleException(
          "Cannot update self-occupancy period: overlaps with an existing period");
    }

    validateNoOverlappingContracts(
        period.getUnitId(), teamId, period.getStartDate(), endDate, null);

    period.setUpdatedAt(Instant.now(clock));
    period.setUpdatedBy(principal.getUserId());
    repository.save(period);

    log.info("Updated self-occupancy period {}", periodIdentifier);
    return toResponse(period, propertyIdentifier, teamId);
  }

  @Transactional
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR')")
  public OccupancyPeriodResponse end(
      PropertyIdentifier propertyIdentifier,
      OccupancyPeriodIdentifier periodIdentifier,
      EndOccupancyPeriodRequest request,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);
    PropertyOccupancyPeriod period = repository.getByIdentifierAndTeamId(periodIdentifier, teamId);

    if (request.endDate().isBefore(period.getStartDate())) {
      throw new BusinessRuleException("End date cannot be before start date");
    }

    period.setEndDate(Optional.of(request.endDate()));
    request.endReason().ifPresent(r -> period.setEndReason(Optional.of(r)));
    request.notes().ifPresent(n -> period.setNotes(Optional.of(n)));
    period.setUpdatedAt(Instant.now(clock));
    period.setUpdatedBy(principal.getUserId());
    repository.save(period);

    // TODO(BUUR-106 Task 12): occupancy status moved from `properties` to `units` in V068.
    // Marking the unit VACANT when the active self-occupancy period ends needs a unit join.
    //
    // The deleted business rule, to reinstate at unit level: only set the unit to VACANT when
    // the end date is today or earlier AND the unit's current status is SELF_OCCUPIED (i.e.
    // `unit.getStatus() == SELF_OCCUPIED`). Do not unconditionally mark VACANT — a unit that is
    // e.g. under maintenance or rented via a separate contract must not be overwritten.
    if (!request.endDate().isAfter(LocalDate.now(clock))) {
      log.warn(
          "Skipping automatic unit status update to VACANT for property {}: status now lives on"
              + " units, not properties (BUUR-106 Task 12)",
          propertyIdentifier);
    }

    log.info("Ended self-occupancy period {}", periodIdentifier);
    return toResponse(period, propertyIdentifier, teamId);
  }

  @Transactional
  @PreAuthorize("hasRole('TEAM_ADMIN')")
  public void delete(
      PropertyIdentifier propertyIdentifier,
      OccupancyPeriodIdentifier periodIdentifier,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);
    PropertyOccupancyPeriod period = repository.getByIdentifierAndTeamId(periodIdentifier, teamId);

    repository.softDeleteByIdAndTeamId(period.getId(), teamId);

    // If active period was deleted, set property to VACANT
    boolean wasActive =
        !period.getStartDate().isAfter(LocalDate.now(clock))
            && (period.getEndDate().isEmpty()
                || !period.getEndDate().get().isBefore(LocalDate.now(clock)));

    // TODO(BUUR-106 Task 12): occupancy status moved from `properties` to `units` in V068.
    // Marking the unit VACANT when an active self-occupancy period is deleted needs a unit join.
    //
    // The deleted business rule, to reinstate at unit level: only set the unit to VACANT when
    // the deleted period was active (wasActive, computed above) AND the unit's current status
    // is SELF_OCCUPIED (i.e. `unit.getStatus() == SELF_OCCUPIED`). Do not unconditionally mark
    // VACANT — a unit that is e.g. under maintenance or rented via a separate contract must not
    // be overwritten.
    if (wasActive) {
      log.warn(
          "Skipping automatic unit status update to VACANT for property {}: status now lives on"
              + " units, not properties (BUUR-106 Task 12)",
          propertyIdentifier);
    }

    log.info("Deleted self-occupancy period {}", periodIdentifier);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public List<OccupancyPeriodResponse> findByProperty(
      PropertyIdentifier propertyIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);
    List<PropertyOccupancyPeriod> periods =
        repository.findByPropertyIdAndTeamId(property.getId(), teamId);

    // Batch-load units so listing a property's periods doesn't do one unit lookup per period.
    List<UUID> unitIds =
        periods.stream().map(PropertyOccupancyPeriod::getUnitId).distinct().toList();
    java.util.Map<UUID, Sid> unitIdentifiersById =
        unitRepository.findByIdsAndTeamId(unitIds, teamId).stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    Unit::getId, u -> u.getIdentifier().orElseThrow()));

    return periods.stream()
        .map(
            p ->
                toResponse(
                    p,
                    propertyIdentifier,
                    Optional.ofNullable(unitIdentifiersById.get(p.getUnitId())).orElseThrow()))
        .toList();
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public OccupancyPeriodResponse get(
      PropertyIdentifier propertyIdentifier,
      OccupancyPeriodIdentifier periodIdentifier,
      UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);
    PropertyOccupancyPeriod period = repository.getByIdentifierAndTeamId(periodIdentifier, teamId);
    return toResponse(period, propertyIdentifier, teamId);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public PropertyTimelineResponse getTimeline(
      PropertyIdentifier propertyIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);

    Optional<LocalDate> acquisitionDate =
        acquisitionRepository
            .findByPropertyIdAndTeamId(property.getId(), teamId)
            .flatMap(PropertyAcquisition::getAcquisitionDate);

    List<PropertyOccupancyPeriod> periods =
        repository.findByPropertyIdAndTeamId(property.getId(), teamId);
    List<Contract> contracts = contractRepository.findByPropertyId(property.getId(), teamId);
    List<PropertyFinancing> financings =
        financingRepository.findByPropertyIdAndTeamId(property.getId(), teamId);

    // Batch-load extensions for effective end date computation
    List<UUID> contractIds = contracts.stream().map(Contract::getId).toList();
    List<ContractExtension> allExtensions =
        contractExtensionRepository.findByContractIdsAndTeamId(contractIds, teamId);
    java.util.Map<UUID, List<ContractExtension>> extensionsByContract =
        allExtensions.stream()
            .collect(java.util.stream.Collectors.groupingBy(ContractExtension::getContractId));

    List<TimelineEntry> entries = new ArrayList<>();

    // Add occupancy periods
    for (PropertyOccupancyPeriod p : periods) {
      entries.add(
          new TimelineEntry(
              TimelineEntryType.SELF_OCCUPANCY,
              p.getIdentifier().orElseThrow(),
              p.getStartDate(),
              p.getEndDate(),
              Optional.of(p.getType().name()),
              p.getOccupantName()));
    }

    // Add contracts
    for (Contract c : contracts) {
      if (c.getStartDate() == null) {
        continue;
      }
      List<ContractExtension> exts = extensionsByContract.getOrDefault(c.getId(), List.of());
      Optional<LocalDate> effectiveEndDate =
          EffectiveEndDateHelper.computeEffectiveEndDate(c.getEndDate(), exts);
      entries.add(
          new TimelineEntry(
              TimelineEntryType.CONTRACT,
              c.getIdentifier().orElseThrow(),
              c.getStartDate(),
              effectiveEndDate,
              Optional.of(c.getStatus().name()),
              Optional.of(c.getContractType().name())));
    }

    // Sort by start date descending
    entries.sort(Comparator.comparing(TimelineEntry::startDate).reversed());

    // Map financing entries
    List<PropertyTimelineResponse.FinancingEntry> financingEntries =
        financings.stream()
            .map(
                f ->
                    new PropertyTimelineResponse.FinancingEntry(
                        f.getIdentifier().orElseThrow(),
                        f.getStartDate(),
                        f.getEndDate(),
                        f.getFinancingType().name(),
                        f.getStatus().name(),
                        f.getLenderName(),
                        f.getOriginalAmount().value(),
                        f.getOriginalAmount().currency(),
                        f.getInterestRate()))
            .toList();

    return new PropertyTimelineResponse(acquisitionDate, entries, financingEntries);
  }

  private void validateNoOverlappingContracts(
      UUID unitId, UUID teamId, LocalDate startDate, LocalDate endDate, @Nullable UUID excludeId) {
    List<Contract> contracts = contractRepository.findByUnitId(unitId, teamId);

    // Batch-load extensions for effective end date computation
    List<UUID> contractIds = contracts.stream().map(Contract::getId).toList();
    List<ContractExtension> allExtensions =
        contractExtensionRepository.findByContractIdsAndTeamId(contractIds, teamId);
    java.util.Map<UUID, List<ContractExtension>> extensionsByContract =
        allExtensions.stream()
            .collect(java.util.stream.Collectors.groupingBy(ContractExtension::getContractId));

    for (Contract c : contracts) {
      if (c.getStartDate() == null) {
        continue;
      }
      if (c.getStatus() == Contract.ContractStatus.DRAFT
          || c.getStatus() == Contract.ContractStatus.PENDING_SIGNATURE) {
        continue;
      }
      List<ContractExtension> exts = extensionsByContract.getOrDefault(c.getId(), List.of());
      Optional<LocalDate> effectiveEndDate =
          EffectiveEndDateHelper.computeEffectiveEndDate(c.getEndDate(), exts);
      LocalDate contractEnd = effectiveEndDate.orElse(LocalDate.of(9999, 12, 31));
      boolean overlaps = !startDate.isAfter(contractEnd) && !endDate.isBefore(c.getStartDate());
      if (overlaps) {
        String endStr = effectiveEndDate.map(LocalDate::toString).orElse("ongoing");
        throw new BusinessRuleException(
            "Cannot create self-occupancy period: overlaps with contract "
                + c.getIdentifier().orElseThrow()
                + " ("
                + c.getStartDate()
                + " → "
                + endStr
                + ")");
      }
    }
  }

  /**
   * Resolves which unit of {@code property} a self-occupancy period is for. An explicit {@code
   * unitIdentifier} must belong to this property (never a silent cross-property period) and to this
   * team ({@link UnitRepository#getByIdentifierAndTeamId} throws {@link
   * com.buurman.exception.NotFoundException} for a wrong-team lookup, which maps to 404 without
   * leaking existence). Omitting it only works when the property has exactly one unit.
   */
  private Unit resolveUnit(
      Property property, @Nullable String unitIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    if (unitIdentifier != null) {
      Unit unit = unitRepository.getByIdentifierAndTeamId(Sid.of(unitIdentifier), teamId);
      if (!unit.getPropertyId().equals(property.getId())) {
        throw new BadRequestException("The chosen unit does not belong to this property.");
      }
      return unit;
    }
    List<Unit> units = unitRepository.findAllByPropertyIdAndTeamId(property.getId(), teamId);
    if (units.size() != 1) {
      throw new BadRequestException(
          "Property "
              + property.getStreet()
              + " has "
              + units.size()
              + " units. Specify which unit the occupancy period is for.");
    }
    return units.get(0);
  }

  private OccupancyPeriodResponse toResponse(
      PropertyOccupancyPeriod period, Sid propertyIdentifier, UUID teamId) {
    Unit unit = unitRepository.getByIdAndTeamId(period.getUnitId(), teamId);
    return toResponse(period, propertyIdentifier, unit.getIdentifier().orElseThrow());
  }

  private OccupancyPeriodResponse toResponse(
      PropertyOccupancyPeriod period, Sid propertyIdentifier, Sid unitIdentifier) {
    return new OccupancyPeriodResponse(
        period.getIdentifier().orElseThrow(),
        propertyIdentifier,
        unitIdentifier,
        period.getStartDate(),
        period.getEndDate(),
        period.getType(),
        period.getOccupantName(),
        period.getMonthlyImputedRent(),
        period.getEndReason(),
        period.getNotes(),
        period.getCreatedAt(),
        Optional.of(period.getUpdatedAt()));
  }
}
