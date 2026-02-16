package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.PROPERTY_TENANT_HISTORY;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.PropertyTenantHistory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PropertyTenantHistoryRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public List<PropertyTenantHistory> findByTenantId(UUID tenantId, UUID teamId) {
    return dsl.selectFrom(PROPERTY_TENANT_HISTORY)
        .where(
            PROPERTY_TENANT_HISTORY
                .TENANT_ID
                .eq(tenantId)
                .and(PROPERTY_TENANT_HISTORY.TEAM_ID.eq(teamId)))
        .orderBy(PROPERTY_TENANT_HISTORY.PERFORMED_AT.desc())
        .fetch()
        .map(
            record ->
                new PropertyTenantHistory(
                    record.getId(),
                    record.getTeamId(),
                    record.getPropertyId(),
                    record.getTenantId(),
                    record.getMovedInAt() != null ? record.getMovedInAt().toInstant(UTC) : null,
                    record.getMovedOutAt() != null ? record.getMovedOutAt().toInstant(UTC) : null,
                    PropertyTenantHistory.ActionType.valueOf(record.getActionType()),
                    record.getPerformedBy(),
                    record.getPerformedAt().toInstant(UTC)));
  }

  public List<PropertyTenantHistory> findByPropertyId(UUID propertyId, UUID teamId) {
    return dsl.selectFrom(PROPERTY_TENANT_HISTORY)
        .where(
            PROPERTY_TENANT_HISTORY
                .PROPERTY_ID
                .eq(propertyId)
                .and(PROPERTY_TENANT_HISTORY.TEAM_ID.eq(teamId)))
        .orderBy(PROPERTY_TENANT_HISTORY.PERFORMED_AT.desc())
        .fetch()
        .map(
            record ->
                new PropertyTenantHistory(
                    record.getId(),
                    record.getTeamId(),
                    record.getPropertyId(),
                    record.getTenantId(),
                    record.getMovedInAt() != null ? record.getMovedInAt().toInstant(UTC) : null,
                    record.getMovedOutAt() != null ? record.getMovedOutAt().toInstant(UTC) : null,
                    PropertyTenantHistory.ActionType.valueOf(record.getActionType()),
                    record.getPerformedBy(),
                    record.getPerformedAt().toInstant(UTC)));
  }

  public void save(PropertyTenantHistory history) {
    LocalDateTime now = LocalDateTime.now(clock);

    dsl.insertInto(PROPERTY_TENANT_HISTORY)
        .set(
            PROPERTY_TENANT_HISTORY.ID,
            history.getId() != null ? history.getId() : UUID.randomUUID())
        .set(PROPERTY_TENANT_HISTORY.TEAM_ID, history.getTeamId())
        .set(PROPERTY_TENANT_HISTORY.PROPERTY_ID, history.getPropertyId())
        .set(PROPERTY_TENANT_HISTORY.TENANT_ID, history.getTenantId())
        .set(
            PROPERTY_TENANT_HISTORY.MOVED_IN_AT,
            history.getMovedInAt() != null
                ? LocalDateTime.ofInstant(history.getMovedInAt(), UTC)
                : null)
        .set(
            PROPERTY_TENANT_HISTORY.MOVED_OUT_AT,
            history.getMovedOutAt() != null
                ? LocalDateTime.ofInstant(history.getMovedOutAt(), UTC)
                : null)
        .set(PROPERTY_TENANT_HISTORY.ACTION_TYPE, history.getActionType().name())
        .set(PROPERTY_TENANT_HISTORY.PERFORMED_BY, history.getPerformedBy())
        .set(
            PROPERTY_TENANT_HISTORY.PERFORMED_AT,
            history.getPerformedAt() != null
                ? LocalDateTime.ofInstant(history.getPerformedAt(), UTC)
                : now)
        .execute();
  }
}
