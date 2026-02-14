package com.buurman.repository;

import com.buurman.domain.PropertyTenantHistory;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.PROPERTY_TENANT_HISTORY;

@Repository
public class PropertyTenantHistoryRepository {

    private final DSLContext dsl;
    private final Clock clock;

    public PropertyTenantHistoryRepository(DSLContext dsl, Clock clock) {
        this.dsl = dsl;
        this.clock = clock;
    }

    public List<PropertyTenantHistory> findByTenantId(UUID tenantId, UUID teamId) {
        return dsl.selectFrom(PROPERTY_TENANT_HISTORY)
                .where(PROPERTY_TENANT_HISTORY.TENANT_ID.eq(tenantId)
                        .and(PROPERTY_TENANT_HISTORY.TEAM_ID.eq(teamId)))
                .orderBy(PROPERTY_TENANT_HISTORY.PERFORMED_AT.desc())
                .fetch()
                .map(record -> new PropertyTenantHistory(
                        record.getId(),
                        record.getTeamId(),
                        record.getPropertyId(),
                        record.getTenantId(),
                        record.getMovedInAt() != null ? record.getMovedInAt().toInstant(ZoneOffset.UTC) : null,
                        record.getMovedOutAt() != null ? record.getMovedOutAt().toInstant(ZoneOffset.UTC) : null,
                        PropertyTenantHistory.ActionType.valueOf(record.getActionType()),
                        record.getPerformedBy(),
                        record.getPerformedAt().toInstant(ZoneOffset.UTC)
                ));
    }

    public List<PropertyTenantHistory> findByPropertyId(UUID propertyId, UUID teamId) {
        return dsl.selectFrom(PROPERTY_TENANT_HISTORY)
                .where(PROPERTY_TENANT_HISTORY.PROPERTY_ID.eq(propertyId)
                        .and(PROPERTY_TENANT_HISTORY.TEAM_ID.eq(teamId)))
                .orderBy(PROPERTY_TENANT_HISTORY.PERFORMED_AT.desc())
                .fetch()
                .map(record -> new PropertyTenantHistory(
                        record.getId(),
                        record.getTeamId(),
                        record.getPropertyId(),
                        record.getTenantId(),
                        record.getMovedInAt() != null ? record.getMovedInAt().toInstant(ZoneOffset.UTC) : null,
                        record.getMovedOutAt() != null ? record.getMovedOutAt().toInstant(ZoneOffset.UTC) : null,
                        PropertyTenantHistory.ActionType.valueOf(record.getActionType()),
                        record.getPerformedBy(),
                        record.getPerformedAt().toInstant(ZoneOffset.UTC)
                ));
    }

    public void save(PropertyTenantHistory history) {
        LocalDateTime now = LocalDateTime.now(clock);

        dsl.insertInto(PROPERTY_TENANT_HISTORY)
                .set(PROPERTY_TENANT_HISTORY.ID, history.getId() != null ? history.getId() : UUID.randomUUID())
                .set(PROPERTY_TENANT_HISTORY.TEAM_ID, history.getTeamId())
                .set(PROPERTY_TENANT_HISTORY.PROPERTY_ID, history.getPropertyId())
                .set(PROPERTY_TENANT_HISTORY.TENANT_ID, history.getTenantId())
                .set(PROPERTY_TENANT_HISTORY.MOVED_IN_AT, history.getMovedInAt() != null ?
                        LocalDateTime.ofInstant(history.getMovedInAt(), ZoneOffset.UTC) : null)
                .set(PROPERTY_TENANT_HISTORY.MOVED_OUT_AT, history.getMovedOutAt() != null ?
                        LocalDateTime.ofInstant(history.getMovedOutAt(), ZoneOffset.UTC) : null)
                .set(PROPERTY_TENANT_HISTORY.ACTION_TYPE, history.getActionType().name())
                .set(PROPERTY_TENANT_HISTORY.PERFORMED_BY, history.getPerformedBy())
                .set(PROPERTY_TENANT_HISTORY.PERFORMED_AT, history.getPerformedAt() != null ?
                        LocalDateTime.ofInstant(history.getPerformedAt(), ZoneOffset.UTC) : now)
                .execute();
    }
}
