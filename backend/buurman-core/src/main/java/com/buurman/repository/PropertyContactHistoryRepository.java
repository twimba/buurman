package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.PROPERTY_CONTACT_HISTORY;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.PropertyContactHistory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PropertyContactHistoryRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public List<PropertyContactHistory> findByContactId(UUID contactId, UUID teamId) {
    return List.copyOf(
        dsl.selectFrom(PROPERTY_CONTACT_HISTORY)
            .where(
                PROPERTY_CONTACT_HISTORY
                    .CONTACT_ID
                    .eq(contactId)
                    .and(PROPERTY_CONTACT_HISTORY.TEAM_ID.eq(teamId)))
            .orderBy(PROPERTY_CONTACT_HISTORY.PERFORMED_AT.desc())
            .fetch()
            .map(
                record ->
                    new PropertyContactHistory(
                        record.getId(),
                        record.getTeamId(),
                        record.getPropertyId(),
                        record.getContactId(),
                        Optional.ofNullable(record.getMovedInAt()).map(dt -> dt.toInstant(UTC)),
                        Optional.ofNullable(record.getMovedOutAt()).map(dt -> dt.toInstant(UTC)),
                        PropertyContactHistory.ActionType.valueOf(record.getActionType()),
                        record.getPerformedBy(),
                        record.getPerformedAt().toInstant(UTC))));
  }

  public List<PropertyContactHistory> findByPropertyId(UUID propertyId, UUID teamId) {
    return List.copyOf(
        dsl.selectFrom(PROPERTY_CONTACT_HISTORY)
            .where(
                PROPERTY_CONTACT_HISTORY
                    .PROPERTY_ID
                    .eq(propertyId)
                    .and(PROPERTY_CONTACT_HISTORY.TEAM_ID.eq(teamId)))
            .orderBy(PROPERTY_CONTACT_HISTORY.PERFORMED_AT.desc())
            .fetch()
            .map(
                record ->
                    new PropertyContactHistory(
                        record.getId(),
                        record.getTeamId(),
                        record.getPropertyId(),
                        record.getContactId(),
                        Optional.ofNullable(record.getMovedInAt()).map(dt -> dt.toInstant(UTC)),
                        Optional.ofNullable(record.getMovedOutAt()).map(dt -> dt.toInstant(UTC)),
                        PropertyContactHistory.ActionType.valueOf(record.getActionType()),
                        record.getPerformedBy(),
                        record.getPerformedAt().toInstant(UTC))));
  }

  public void save(PropertyContactHistory history) {
    LocalDateTime now = LocalDateTime.now(clock);

    dsl.insertInto(PROPERTY_CONTACT_HISTORY)
        .set(
            PROPERTY_CONTACT_HISTORY.ID,
            history.getId() != null ? history.getId() : UUID.randomUUID())
        .set(PROPERTY_CONTACT_HISTORY.TEAM_ID, history.getTeamId())
        .set(PROPERTY_CONTACT_HISTORY.PROPERTY_ID, history.getPropertyId())
        .set(PROPERTY_CONTACT_HISTORY.CONTACT_ID, history.getContactId())
        .set(
            PROPERTY_CONTACT_HISTORY.MOVED_IN_AT,
            history.getMovedInAt().map(i -> LocalDateTime.ofInstant(i, UTC)).orElse(null))
        .set(
            PROPERTY_CONTACT_HISTORY.MOVED_OUT_AT,
            history.getMovedOutAt().map(i -> LocalDateTime.ofInstant(i, UTC)).orElse(null))
        .set(PROPERTY_CONTACT_HISTORY.ACTION_TYPE, history.getActionType().name())
        .set(PROPERTY_CONTACT_HISTORY.PERFORMED_BY, history.getPerformedBy())
        .set(
            PROPERTY_CONTACT_HISTORY.PERFORMED_AT,
            history.getPerformedAt() != null
                ? LocalDateTime.ofInstant(history.getPerformedAt(), UTC)
                : now)
        .execute();
  }
}
