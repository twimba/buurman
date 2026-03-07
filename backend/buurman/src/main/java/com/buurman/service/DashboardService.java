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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.buurman.domain.ContractIncomeEntry;
import com.buurman.domain.Property;
import com.buurman.domain.Sid;
import com.buurman.dto.response.DashboardStatsResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.repository.AuditLogRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

  private final AuditLogRepository auditLogRepository;
  private final PropertyRepository propertyRepository;
  private final ContractRepository contractRepository;
  private final TeamService teamService;

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
}
