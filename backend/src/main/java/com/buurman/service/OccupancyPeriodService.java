package com.buurman.service;

import static com.buurman.domain.Property.PropertyStatus.SELF_OCCUPIED;
import static com.buurman.domain.Property.PropertyStatus.VACANT;
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
import com.buurman.domain.Property;
import com.buurman.domain.PropertyFinancing;
import com.buurman.domain.PropertyOccupancyPeriod;
import com.buurman.domain.Sid;
import com.buurman.domain.identifier.OccupancyPeriodIdentifier;
import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.dto.request.CreateOccupancyPeriodRequest;
import com.buurman.dto.request.EndOccupancyPeriodRequest;
import com.buurman.dto.request.UpdateOccupancyPeriodRequest;
import com.buurman.dto.response.OccupancyPeriodResponse;
import com.buurman.dto.response.PropertyTimelineResponse;
import com.buurman.dto.response.PropertyTimelineResponse.TimelineEntry;
import com.buurman.dto.response.PropertyTimelineResponse.TimelineEntryType;
import com.buurman.exception.BusinessRuleException;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyAcquisitionRepository;
import com.buurman.repository.PropertyFinancingRepository;
import com.buurman.repository.PropertyOccupancyPeriodRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class OccupancyPeriodService {

  private final PropertyOccupancyPeriodRepository repository;
  private final PropertyRepository propertyRepository;
  private final ContractRepository contractRepository;
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

    // Validate no overlapping occupancy periods
    LocalDate endDate = request.endDate().orElse(LocalDate.of(9999, 12, 31));
    List<PropertyOccupancyPeriod> overlapping =
        repository.findOverlapping(property.getId(), teamId, request.startDate(), endDate, null);
    if (!overlapping.isEmpty()) {
      throw new BusinessRuleException(
          "Cannot create self-occupancy period: overlaps with an existing period");
    }

    // Validate no overlapping contracts
    validateNoOverlappingContracts(property.getId(), teamId, request.startDate(), endDate, null);

    Instant now = Instant.now(clock);
    PropertyOccupancyPeriod period =
        PropertyOccupancyPeriod.builder()
            .identifier(Optional.of(newOccupancyPeriodId()))
            .teamId(teamId)
            .propertyId(property.getId())
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

    // Update property status if period starts today or earlier
    if (!request.startDate().isAfter(LocalDate.now(clock))) {
      property.setStatus(SELF_OCCUPIED);
      propertyRepository.save(property);
    }

    log.info(
        "Created self-occupancy period {} for property {}",
        period.getIdentifier().orElseThrow(),
        propertyIdentifier);

    return toResponse(period, propertyIdentifier);
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

    // Validate no overlapping periods (excluding self)
    LocalDate endDate = period.getEndDate().orElse(LocalDate.of(9999, 12, 31));
    List<PropertyOccupancyPeriod> overlapping =
        repository.findOverlapping(
            property.getId(), teamId, period.getStartDate(), endDate, period.getId());
    if (!overlapping.isEmpty()) {
      throw new BusinessRuleException(
          "Cannot update self-occupancy period: overlaps with an existing period");
    }

    validateNoOverlappingContracts(property.getId(), teamId, period.getStartDate(), endDate, null);

    period.setUpdatedAt(Instant.now(clock));
    period.setUpdatedBy(principal.getUserId());
    repository.save(period);

    log.info("Updated self-occupancy period {}", periodIdentifier);
    return toResponse(period, propertyIdentifier);
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

    // If the period was active, set property to VACANT
    if (!request.endDate().isAfter(LocalDate.now(clock)) && property.getStatus() == SELF_OCCUPIED) {
      property.setStatus(VACANT);
      propertyRepository.save(property);
    }

    log.info("Ended self-occupancy period {}", periodIdentifier);
    return toResponse(period, propertyIdentifier);
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

    if (wasActive && property.getStatus() == SELF_OCCUPIED) {
      property.setStatus(VACANT);
      propertyRepository.save(property);
    }

    log.info("Deleted self-occupancy period {}", periodIdentifier);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public List<OccupancyPeriodResponse> findByProperty(
      PropertyIdentifier propertyIdentifier, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);
    return repository.findByPropertyIdAndTeamId(property.getId(), teamId).stream()
        .map(p -> toResponse(p, propertyIdentifier))
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
    return toResponse(period, propertyIdentifier);
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
            .flatMap(a -> a.getAcquisitionDate());

    List<PropertyOccupancyPeriod> periods =
        repository.findByPropertyIdAndTeamId(property.getId(), teamId);
    List<Contract> contracts = contractRepository.findByPropertyId(property.getId(), teamId);
    List<PropertyFinancing> financings =
        financingRepository.findByPropertyIdAndTeamId(property.getId(), teamId);

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
      entries.add(
          new TimelineEntry(
              TimelineEntryType.CONTRACT,
              c.getIdentifier().orElseThrow(),
              c.getStartDate(),
              c.getEndDate(),
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
      UUID propertyId,
      UUID teamId,
      LocalDate startDate,
      LocalDate endDate,
      @Nullable UUID excludeId) {
    List<Contract> contracts = contractRepository.findByPropertyId(propertyId, teamId);
    for (Contract c : contracts) {
      if (c.getStartDate() == null) {
        continue;
      }
      if (c.getStatus() == Contract.ContractStatus.DRAFT
          || c.getStatus() == Contract.ContractStatus.PENDING_SIGNATURE) {
        continue;
      }
      LocalDate contractEnd = c.getEndDate().orElse(LocalDate.of(9999, 12, 31));
      boolean overlaps = !startDate.isAfter(contractEnd) && !endDate.isBefore(c.getStartDate());
      if (overlaps) {
        String endStr = c.getEndDate().map(LocalDate::toString).orElse("ongoing");
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

  private OccupancyPeriodResponse toResponse(
      PropertyOccupancyPeriod period, Sid propertyIdentifier) {
    return new OccupancyPeriodResponse(
        period.getIdentifier().orElseThrow(),
        propertyIdentifier,
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
