package com.buurman.service;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.time.ZoneOffset.UTC;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.buurman.domain.Contact;
import com.buurman.domain.Contract;
import com.buurman.domain.ContractExtension;
import com.buurman.domain.ContractIncomeEntry;
import com.buurman.domain.ContractParty;
import com.buurman.domain.ContractPartyRole;
import com.buurman.domain.Property;
import com.buurman.domain.Sid;
import com.buurman.domain.Unit;
import com.buurman.domain.UnitStatus;
import com.buurman.dto.response.ContractExtensionResponse;
import com.buurman.dto.response.DashboardStatsResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.generated.model.RenewalMode;
import com.buurman.generated.model.UpcomingRenewalResponse;
import com.buurman.repository.AuditLogRepository;
import com.buurman.repository.ContactRepository;
import com.buurman.repository.ContractExtensionRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.UnitRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

  private final AuditLogRepository auditLogRepository;
  private final PropertyRepository propertyRepository;
  private final UnitRepository unitRepository;
  private final ContractRepository contractRepository;
  private final ContractExtensionRepository extensionRepository;
  private final ContactRepository contactRepository;
  private final ContractPartyService contractPartyService;
  private final TeamService teamService;
  private final Clock clock;

  /**
   * Occupancy status moved from {@code properties} to {@code units} in V070, so every count below
   * (occupied, vacant, maintenance, etc.) and both occupancy rates are now a breakdown of the
   * team's units, not its properties. {@code totalProperties} alone still counts properties.
   */
  public DashboardStatsResponse getDashboardStats(UUID teamId) {
    int totalProperties = propertyRepository.countByTeamId(teamId);
    List<Unit> units = unitRepository.findAllByTeamId(teamId);

    int occupiedUnits = countByStatus(units, UnitStatus.OCCUPIED);
    int selfOccupiedUnits = countByStatus(units, UnitStatus.SELF_OCCUPIED);
    int vacantUnits = countByStatus(units, UnitStatus.VACANT);
    int maintenanceUnits = countByStatus(units, UnitStatus.MAINTENANCE);
    int unavailableUnits = countByStatus(units, UnitStatus.UNAVAILABLE);
    int underRenovationUnits = countByStatus(units, UnitStatus.UNDER_RENOVATION);
    int fallowUnits = countByStatus(units, UnitStatus.FALLOW);
    int listedUnits = countByStatus(units, UnitStatus.LISTED);

    int totalUnits = units.size();

    // Occupancy rate: occupied + self-occupied vs available (excluding unavailable)
    int availableUnits = totalUnits - unavailableUnits;
    BigDecimal occupancyRate =
        availableUnits > 0
            ? BigDecimal.valueOf(occupiedUnits + selfOccupiedUnits)
                .divide(BigDecimal.valueOf(availableUnits), 4, HALF_UP)
                .multiply(BigDecimal.valueOf(100))
            : ZERO;

    // Rental occupancy rate: only rented units vs rental-eligible units.
    // Excludes self-occupied from both numerator and denominator.
    int rentalEligibleUnits = totalUnits - unavailableUnits - selfOccupiedUnits;
    BigDecimal rentalOccupancyRate =
        rentalEligibleUnits > 0
            ? BigDecimal.valueOf(occupiedUnits)
                .divide(BigDecimal.valueOf(rentalEligibleUnits), 4, HALF_UP)
                .multiply(BigDecimal.valueOf(100))
            : ZERO;

    DashboardStatsResponse.MonthlyIncome monthlyIncome = calculateMonthlyIncome(teamId);

    return new DashboardStatsResponse(
        totalProperties,
        totalUnits,
        occupiedUnits,
        selfOccupiedUnits,
        vacantUnits,
        maintenanceUnits,
        unavailableUnits,
        underRenovationUnits,
        fallowUnits,
        listedUnits,
        monthlyIncome,
        occupancyRate,
        rentalOccupancyRate);
  }

  private static int countByStatus(List<Unit> units, UnitStatus status) {
    return (int) units.stream().filter(u -> u.getStatus() == status).count();
  }

  private DashboardStatsResponse.MonthlyIncome calculateMonthlyIncome(UUID teamId) {
    List<ContractIncomeEntry> activeContracts =
        contractRepository.findInForceContractIncomeByTeamId(teamId);

    if (activeContracts.isEmpty()) {
      return new DashboardStatsResponse.MonthlyIncome(ZERO, teamService.getDefaultCurrency(teamId));
    }

    // Group by currency and calculate monthly income
    Map<String, BigDecimal> incomePerCurrency = new HashMap<>();

    for (var contract : activeContracts) {
      String currency = contract.rentAmountCurrency();
      BigDecimal rentAmount = contract.rentAmount();
      String paymentFrequency = contract.paymentFrequency();

      // Convert to monthly amount based on payment frequency
      BigDecimal monthlyAmount =
          switch (paymentFrequency) {
            case "MONTHLY" -> rentAmount;
            case "QUARTERLY" -> rentAmount.divide(BigDecimal.valueOf(3), 2, HALF_UP);
            case "ANNUALLY" -> rentAmount.divide(BigDecimal.valueOf(12), 2, HALF_UP);
            default -> rentAmount;
          };

      incomePerCurrency.merge(currency, monthlyAmount, BigDecimal::add);
    }

    // For simplicity, return the first currency (typically EUR)
    Map.Entry<String, BigDecimal> primaryIncome = incomePerCurrency.entrySet().iterator().next();

    return new DashboardStatsResponse.MonthlyIncome(
        primaryIncome.getValue().setScale(2, HALF_UP), primaryIncome.getKey());
  }

  public List<RecentActivityResponse> getRecentActivities(UUID teamId, int limit) {
    return auditLogRepository.findRecentByTeamId(teamId, limit).stream()
        .map(
            record -> {
              String entityType = record.entityType();
              UUID entityId = record.entityId();
              String action = record.action();
              String userName =
                  record
                      .firstName()
                      .flatMap(fn -> record.lastName().map(ln -> fn + " " + ln))
                      .orElse("Unknown");

              // Get entity name and identifier based on type
              String entityName =
                  auditLogRepository.findEntityName(entityType, entityId, teamId).orElse("Unknown");
              Sid entityIdentifier =
                  auditLogRepository
                      .findEntityIdentifier(entityType, entityId, teamId)
                      .orElse(Sid.of(entityId.toString()));

              // Build description
              String description =
                  buildActivityDescription(action, entityType, entityName, userName);

              Instant instant = record.timestamp().toInstant(UTC);

              return new RecentActivityResponse(
                  entityType, entityIdentifier, entityName, action, userName, instant, description);
            })
        .toList();
  }

  private String buildActivityDescription(
      String action, String entityType, String entityName, String userName) {
    String actionText =
        switch (action) {
          case "CREATE" -> "created";
          case "UPDATE" -> "updated";
          case "DELETE" -> "deleted";
          case "RESTORE" -> "restored";
          default -> "modified";
        };

    return String.format("%s %s %s", userName, actionText, entityName);
  }

  public List<UpcomingRenewalResponse> getUpcomingRenewals(UUID teamId) {
    LocalDate today = LocalDate.now(clock);
    LocalDate horizon = today.plusDays(90);

    // Find ACTIVE, FIXED_TERM contracts with renewal_mode != NONE
    List<Contract> contracts =
        contractRepository.findActiveByTeamId(teamId).stream()
            .filter(c -> c.getRenewalMode() != Contract.RenewalMode.NONE)
            .filter(c -> c.getContractType() == Contract.ContractType.FIXED_TERM)
            .toList();

    if (contracts.isEmpty()) {
      return List.of();
    }

    // Batch load extensions
    List<UUID> contractIds = contracts.stream().map(Contract::getId).toList();
    List<ContractExtension> allExtensions =
        extensionRepository.findByContractIdsAndTeamId(contractIds, teamId);
    Map<UUID, List<ContractExtension>> extensionsByContract =
        allExtensions.stream().collect(Collectors.groupingBy(ContractExtension::getContractId));

    // Batch load properties
    List<UUID> propertyIds = contracts.stream().map(Contract::getPropertyId).distinct().toList();
    Map<UUID, Property> propertyMap =
        propertyRepository.findByIdsAndTeamId(propertyIds, teamId).stream()
            .collect(Collectors.toMap(Property::getId, p -> p));

    // Batch load parties for primary contact lookup
    Map<UUID, List<ContractParty>> partiesByContract =
        contractPartyService.getPartiesForContracts(contractIds, teamId);

    // Batch load contacts for all parties
    List<UUID> allContactIds =
        partiesByContract.values().stream()
            .flatMap(List::stream)
            .flatMap(p -> p.getContactId().stream())
            .distinct()
            .toList();
    Map<UUID, Contact> contactMap =
        contactRepository.findByIdsAndTeamId(allContactIds, teamId).stream()
            .collect(Collectors.toMap(Contact::getId, c -> c));

    return contracts.stream()
        .map(
            contract -> {
              List<ContractExtension> extensions =
                  extensionsByContract.getOrDefault(contract.getId(), List.of());

              // Compute effective end date
              Optional<LocalDate> effectiveEnd =
                  EffectiveEndDateHelper.computeEffectiveEndDate(contract.getEndDate(), extensions);
              if (effectiveEnd.isEmpty()) {
                return Optional.<UpcomingRenewalResponse>empty();
              }

              LocalDate endDate = effectiveEnd.get();

              // Filter: within 90 days
              if (endDate.isAfter(horizon) || endDate.isBefore(today)) {
                return Optional.<UpcomingRenewalResponse>empty();
              }

              // Filter out contracts that already have a DRAFT or ACTIVE extension
              boolean hasPendingExtension =
                  extensions.stream()
                      .anyMatch(
                          e ->
                              (e.getStatus() == ContractExtension.ExtensionStatus.DRAFT
                                      || e.getStatus() == ContractExtension.ExtensionStatus.ACTIVE)
                                  && e.getDeletedAt().isEmpty());
              if (hasPendingExtension) {
                return Optional.<UpcomingRenewalResponse>empty();
              }

              Property property = propertyMap.get(contract.getPropertyId());
              String propertyName =
                  property != null ? property.getStreet() + ", " + property.getCity() : null;

              // Find primary contact name
              List<ContractParty> parties =
                  partiesByContract.getOrDefault(contract.getId(), List.of());
              String contactName =
                  parties.stream()
                      .filter(p -> p.getRole() == ContractPartyRole.PRIMARY_TENANT)
                      .findFirst()
                      .flatMap(ContractParty::getContactId)
                      .map(contactMap::get)
                      .map(Contact::getDisplayName)
                      .orElse(null);

              int daysUntilExpiry = (int) ChronoUnit.DAYS.between(today, endDate);

              RenewalMode mode = RenewalMode.fromValue(contract.getRenewalMode().name());

              UpcomingRenewalResponse response =
                  new UpcomingRenewalResponse(
                      contract.getIdentifier().orElseThrow().value(),
                      endDate,
                      mode,
                      contract.getRentAmount().value(),
                      contract.getRentAmount().currency(),
                      daysUntilExpiry);
              response.setPropertyName(propertyName);
              response.setContactName(contactName);
              response.setRenewalTermMonths(contract.getRenewalTermMonths().orElse(null));

              return Optional.of(response);
            })
        .flatMap(Optional::stream)
        .sorted(Comparator.comparing(UpcomingRenewalResponse::getEffectiveEndDate))
        .limit(10)
        .toList();
  }

  public List<ContractExtensionResponse> getPendingExtensions(UUID teamId) {
    List<ContractExtension> drafts = extensionRepository.findDraftsByTeamId(teamId);
    if (drafts.isEmpty()) {
      return List.of();
    }

    // Batch-load contracts for all draft extensions
    List<UUID> contractIds =
        drafts.stream().map(ContractExtension::getContractId).distinct().toList();
    Map<UUID, Sid> contractIdentifierMap =
        contractRepository.findByIdsAndTeamId(contractIds, teamId).stream()
            .collect(Collectors.toMap(Contract::getId, c -> c.getIdentifier().orElseThrow()));

    return drafts.stream()
        .filter(ext -> contractIdentifierMap.containsKey(ext.getContractId()))
        .map(
            ext -> {
              Sid contractSid =
                  java.util.Objects.requireNonNull(contractIdentifierMap.get(ext.getContractId()));
              return new ContractExtensionResponse(
                  ext.getIdentifier().orElseThrow(),
                  contractSid,
                  ext.getExtensionNumber(),
                  ext.getPreviousEndDate(),
                  ext.getNewEndDate(),
                  ext.getPreviousRentAmount().value(),
                  ext.getPreviousRentAmount().currency(),
                  ext.getNewRentAmount().value(),
                  ext.getNewRentAmount().currency(),
                  ext.getRentAdjustmentType(),
                  ext.getRentAdjustmentValue(),
                  ext.getStatus(),
                  ext.getTriggerType(),
                  ext.getNotes(),
                  ext.getDeclinedReason(),
                  ext.getActivatedAt(),
                  ext.getConfirmedAt(),
                  ext.getSupersededAt(),
                  ext.getCreatedAt());
            })
        .toList();
  }
}
