package com.buurman.service;

import com.buurman.domain.Property;
import com.buurman.dto.response.DashboardStatsResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.repository.AuditLogRepository;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.PropertyRepository;
import org.jooq.Record;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.buurman.domain.Property.PropertyStatus.MAINTENANCE;
import static com.buurman.domain.Property.PropertyStatus.OCCUPIED;
import static com.buurman.domain.Property.PropertyStatus.UNAVAILABLE;
import static com.buurman.domain.Property.PropertyStatus.VACANT;
import static com.buurman.jooq.generated.Tables.AUDIT_LOG;
import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.USERS;
import static java.time.ZoneOffset.UTC;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AuditLogRepository auditLogRepository;
    private final PropertyRepository propertyRepository;
    private final ContractRepository contractRepository;

    public DashboardStatsResponse getDashboardStats(UUID teamId) {
        List<Property> allProperties = propertyRepository.findAllByTeamId(teamId);

        int totalProperties = allProperties.size();
        int occupiedUnits = (int) allProperties.stream()
                .filter(p -> p.getStatus() == OCCUPIED)
                .count();
        int vacantUnits = (int) allProperties.stream()
                .filter(p -> p.getStatus() == VACANT)
                .count();
        int maintenanceUnits = (int) allProperties.stream()
                .filter(p -> p.getStatus() == MAINTENANCE)
                .count();
        int unavailableUnits = (int) allProperties.stream()
                .filter(p -> p.getStatus() == UNAVAILABLE)
                .count();

        // Calculate occupancy rate (excluding unavailable units)
        int availableUnits = totalProperties - unavailableUnits;
        BigDecimal occupancyRate = availableUnits > 0
                ? BigDecimal.valueOf(occupiedUnits)
                    .divide(BigDecimal.valueOf(availableUnits), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Calculate monthly income from active contracts
        DashboardStatsResponse.MonthlyIncome monthlyIncome = calculateMonthlyIncome(teamId);

        return new DashboardStatsResponse(
                totalProperties,
                occupiedUnits,
                vacantUnits,
                maintenanceUnits,
                unavailableUnits,
                monthlyIncome,
                occupancyRate
        );
    }

    public List<RecentActivityResponse> getRecentActivities(UUID teamId, int limit) {
        return auditLogRepository.findRecentByTeamId(teamId, limit)
                .stream()
                .map(record -> {
                    String entityType = record.get(AUDIT_LOG.ENTITY_TYPE);
                    UUID entityId = record.get(AUDIT_LOG.ENTITY_ID);
                    String action = record.get(AUDIT_LOG.ACTION);
                    String firstName = record.get(USERS.FIRST_NAME);
                    String lastName = record.get(USERS.LAST_NAME);
                    String userName = (firstName != null && lastName != null)
                            ? firstName + " " + lastName
                            : "Unknown";

                    // Get entity name based on type
                    String entityName = auditLogRepository.findEntityName(entityType, entityId, teamId)
                            .orElse("Unknown");

                    // Resolve entity identifier
                    String entityIdentifier = auditLogRepository.findEntityIdentifier(entityType, entityId, teamId)
                            .orElse(entityId != null ? entityId.toString() : "unknown");

                    // Build description
                    String description = buildActivityDescription(action, entityType, entityName, userName);

                    return new RecentActivityResponse(
                            entityType,
                            entityIdentifier,
                            entityName,
                            action,
                            userName,
                            record.get(AUDIT_LOG.TIMESTAMP).toInstant(UTC),
                            description
                    );
                })
                .toList();
    }

    private String buildActivityDescription(String action, String entityType, String entityName, String userName) {
        String actionText = switch (action) {
            case "CREATE" -> "created";
            case "UPDATE" -> "updated";
            case "DELETE" -> "deleted";
            case "RESTORE" -> "restored";
            default -> "modified";
        };

        return String.format("%s %s %s", userName, actionText, entityName);
    }

    private DashboardStatsResponse.MonthlyIncome calculateMonthlyIncome(UUID teamId) {
        List<Record> activeContracts = contractRepository.findActiveContractIncomeByTeamId(teamId);

        if (activeContracts.isEmpty()) {
            return new DashboardStatsResponse.MonthlyIncome(BigDecimal.ZERO, "EUR");
        }

        // Group by currency and calculate monthly income
        Map<String, BigDecimal> incomePerCurrency = new java.util.HashMap<>();

        for (var contract : activeContracts) {
            BigDecimal rentAmount = contract.get(CONTRACTS.RENT_AMOUNT);
            String currency = contract.get(CONTRACTS.CURRENCY);
            String paymentFrequency = contract.get(CONTRACTS.PAYMENT_FREQUENCY);

            // Convert to monthly amount based on payment frequency
            BigDecimal monthlyAmount = switch (paymentFrequency) {
                case "MONTHLY" -> rentAmount;
                case "QUARTERLY" -> rentAmount.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
                case "ANNUALLY" -> rentAmount.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
                default -> rentAmount;
            };

            incomePerCurrency.merge(currency, monthlyAmount, BigDecimal::add);
        }

        // For simplicity, return the first currency (typically EUR)
        Map.Entry<String, BigDecimal> primaryIncome = incomePerCurrency.entrySet().iterator().next();

        return new DashboardStatsResponse.MonthlyIncome(
                primaryIncome.getValue().setScale(2, RoundingMode.HALF_UP),
                primaryIncome.getKey()
        );
    }
}
