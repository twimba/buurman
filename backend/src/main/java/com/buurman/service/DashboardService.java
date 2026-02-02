package com.buurman.service;

import com.buurman.domain.Property;
import com.buurman.dto.response.DashboardStatsResponse;
import com.buurman.dto.response.RecentActivityResponse;
import com.buurman.repository.PropertyRepository;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.*;

@Service
public class DashboardService {

    private final DSLContext dsl;
    private final PropertyRepository propertyRepository;

    public DashboardService(DSLContext dsl, PropertyRepository propertyRepository) {
        this.dsl = dsl;
        this.propertyRepository = propertyRepository;
    }

    public DashboardStatsResponse getDashboardStats(UUID teamId) {
        List<Property> allProperties = propertyRepository.findAllByTeamId(teamId);

        int totalProperties = allProperties.size();
        int occupiedUnits = (int) allProperties.stream()
                .filter(p -> p.getStatus() == Property.PropertyStatus.OCCUPIED)
                .count();
        int vacantUnits = (int) allProperties.stream()
                .filter(p -> p.getStatus() == Property.PropertyStatus.VACANT)
                .count();
        int maintenanceUnits = (int) allProperties.stream()
                .filter(p -> p.getStatus() == Property.PropertyStatus.MAINTENANCE)
                .count();
        int unavailableUnits = (int) allProperties.stream()
                .filter(p -> p.getStatus() == Property.PropertyStatus.UNAVAILABLE)
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
        return dsl.select(
                        AUDIT_LOG.ID,
                        AUDIT_LOG.ENTITY_TYPE,
                        AUDIT_LOG.ENTITY_ID,
                        AUDIT_LOG.ACTION,
                        AUDIT_LOG.TIMESTAMP,
                        USERS.FIRST_NAME,
                        USERS.LAST_NAME
                )
                .from(AUDIT_LOG)
                .leftJoin(USERS).on(AUDIT_LOG.USER_ID.eq(USERS.ID))
                .where(AUDIT_LOG.TEAM_ID.eq(teamId))
                .orderBy(AUDIT_LOG.TIMESTAMP.desc())
                .limit(limit)
                .fetch()
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
                    String entityName = getEntityName(entityType, entityId, teamId);

                    // Build description
                    String description = buildActivityDescription(action, entityType, entityName, userName);

                    return new RecentActivityResponse(
                            record.get(AUDIT_LOG.ID),
                            entityType,
                            entityId,
                            entityName,
                            action,
                            userName,
                            record.get(AUDIT_LOG.TIMESTAMP).toInstant(java.time.ZoneOffset.UTC),
                            description
                    );
                });
    }

    private String getEntityName(String entityType, UUID entityId, UUID teamId) {
        return switch (entityType.toLowerCase()) {
            case "property" -> dsl.select(PROPERTIES.STREET, PROPERTIES.CITY)
                    .from(PROPERTIES)
                    .where(PROPERTIES.ID.eq(entityId)
                            .and(PROPERTIES.TEAM_ID.eq(teamId)))
                    .fetchOptional()
                    .map(r -> r.get(PROPERTIES.STREET) + ", " + r.get(PROPERTIES.CITY))
                    .orElse("Unknown Property");
            case "tenant" -> dsl.select(TENANTS.FIRST_NAME, TENANTS.LAST_NAME)
                    .from(TENANTS)
                    .where(TENANTS.ID.eq(entityId)
                            .and(TENANTS.TEAM_ID.eq(teamId)))
                    .fetchOptional()
                    .map(r -> {
                        String firstName = r.get(TENANTS.FIRST_NAME);
                        String lastName = r.get(TENANTS.LAST_NAME);
                        return lastName != null ? firstName + " " + lastName : firstName;
                    })
                    .orElse("Unknown Tenant");
            case "team" -> dsl.select(TEAMS.NAME)
                    .from(TEAMS)
                    .where(TEAMS.ID.eq(entityId))
                    .fetchOptional()
                    .map(r -> r.get(TEAMS.NAME))
                    .orElse("Unknown Team");
            case "user" -> dsl.select(USERS.FIRST_NAME, USERS.LAST_NAME)
                    .from(USERS)
                    .where(USERS.ID.eq(entityId))
                    .fetchOptional()
                    .map(r -> r.get(USERS.FIRST_NAME) + " " + r.get(USERS.LAST_NAME))
                    .orElse("Unknown User");
            default -> "Unknown";
        };
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
        // Get all active contracts
        var activeContracts = dsl.select(
                        CONTRACTS.RENT_AMOUNT,
                        CONTRACTS.CURRENCY,
                        CONTRACTS.PAYMENT_FREQUENCY
                )
                .from(CONTRACTS)
                .where(CONTRACTS.TEAM_ID.eq(teamId)
                        .and(CONTRACTS.STATUS.eq("ACTIVE"))
                        .and(CONTRACTS.DELETED_AT.isNull()))
                .fetch();

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
        // In a real scenario, you might want to convert all to a base currency
        Map.Entry<String, BigDecimal> primaryIncome = incomePerCurrency.entrySet().iterator().next();

        return new DashboardStatsResponse.MonthlyIncome(
                primaryIncome.getValue().setScale(2, RoundingMode.HALF_UP),
                primaryIncome.getKey()
        );
    }
}
