package com.buurman.service;

import static com.buurman.domain.Property.PropertyStatus.FALLOW;
import static com.buurman.domain.Property.PropertyStatus.LISTED;
import static com.buurman.domain.Property.PropertyStatus.MAINTENANCE;
import static com.buurman.domain.Property.PropertyStatus.OCCUPIED;
import static com.buurman.domain.Property.PropertyStatus.SELF_OCCUPIED;
import static com.buurman.domain.Property.PropertyStatus.UNAVAILABLE;
import static com.buurman.domain.Property.PropertyStatus.UNDER_RENOVATION;
import static com.buurman.domain.Property.PropertyStatus.VACANT;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.time.ZoneOffset.UTC;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

  private final AuditLogRepository auditLogRepository;
  private final PropertyRepository propertyRepository;
  private final ContractRepository contractRepository;
  private final ContractExtensionRepository extensionRepository;
  private final ContactRepository contactRepository;
  private final ContractPartyService contractPartyService;
  private final TeamService teamService;
  private final Clock clock;

  public DashboardStatsResponse getDashboardStats(UUID teamId) {
    List<Property> allProperties = propertyRepository.findAllByTeamId(teamId);

    int totalProperties = allProperties.size();
    int occupiedUnits = (int) allProperties.stream().filter(p -> p.getStatus() == OCCUPIED).count();
    int selfOccupiedUnits =
        (int) allProperties.stream().filter(p -> p.getStatus() == SELF_OCCUPIED).count();
    int vacantUnits = (int) allProperties.stream().filter(p -> p.getStatus() == VACANT).count();
    int maintenanceUnits =
        (int) allProperties.stream().filter(p -> p.getStatus() == MAINTENANCE).count();
    int unavailableUnits =
        (int) allProperties.stream().filter(p -> p.getStatus() == UNAVAILABLE).count();
    int underRenovationUnits =
        (int) allProperties.stream().filter(p -> p.getStatus() == UNDER_RENOVATION).count();
    int fallowUnits = (int) allProperties.stream().filter(p -> p.getStatus() == FALLOW).count();
    int listedUnits = (int) allProperties.stream().filter(p -> p.getStatus() == LISTED).count();

    // Occupancy rate: occupied + self-occupied vs available (excluding unavailable)
    int availableUnits = totalProperties - unavailableUnits;
    BigDecimal occupancyRate =
        availableUnits > 0
            ? BigDecimal.valueOf(occupiedUnits + selfOccupiedUnits)
                .divide(BigDecimal.valueOf(availableUnits), 4, HALF_UP)
                .multiply(BigDecimal.valueOf(100))
            : ZERO;

    // Rental occupancy rate: only rented units vs rental-eligible units
    // Excludes self-occupied from both numerator and denominator
    int rentalEligibleUnits = totalProperties - unavailableUnits - selfOccupiedUnits;
    BigDecimal rentalOccupancyRate =
        rentalEligibleUnits > 0
            ? BigDecimal.valueOf(occupiedUnits)
                .divide(BigDecimal.valueOf(rentalEligibleUnits), 4, HALF_UP)
                .multiply(BigDecimal.valueOf(100))
            : ZERO;

    // Calculate monthly income from active contracts
    DashboardStatsResponse.MonthlyIncome monthlyIncome = calculateMonthlyIncome(teamId);

    return new DashboardStatsResponse(
        totalProperties,
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

  private DashboardStatsResponse.MonthlyIncome calculateMonthlyIncome(UUID teamId) {
    List<ContractIncomeEntry> activeContracts =
        contractRepository.findActiveContractIncomeByTeamId(teamId);

    if (activeContracts.isEmpty()) {
      return new DashboardStatsResponse.MonthlyIncome(ZERO, teamService.getDefaultCurrency(teamId));
    }

    // Group by currency and calculate monthly income
    Map<String, BigDecimal> incomePerCurrency = new java.util.HashMap<>();

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
