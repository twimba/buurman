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

        // Calculate occupancy rate
        BigDecimal occupancyRate = totalProperties > 0
                ? BigDecimal.valueOf(occupiedUnits)
                    .divide(BigDecimal.valueOf(totalProperties), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        // Monthly income - will be implemented in Phase 2 with contracts
        DashboardStatsResponse.MonthlyIncome monthlyIncome =
                new DashboardStatsResponse.MonthlyIncome(BigDecimal.ZERO, "EUR");

        return new DashboardStatsResponse(
                totalProperties,
                occupiedUnits,
                vacantUnits,
                maintenanceUnits,
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
}
