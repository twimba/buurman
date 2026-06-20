package com.buurman.repository.backoffice;

import static com.buurman.jooq.generated.Tables.BACKOFFICE_ACTION_ITEM_SNOOZE;
import static com.buurman.jooq.generated.Tables.BACKOFFICE_USER_DASHBOARD_LAYOUT;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/** Persistence for per-user backoffice dashboard layout + action-item snoozes. */
@Repository
@RequiredArgsConstructor
public class DashboardLayoutRepository {

  private final DSLContext dsl;

  public Optional<String> findLayoutJson(UUID backofficeUserId) {
    return dsl.select(BACKOFFICE_USER_DASHBOARD_LAYOUT.LAYOUT)
        .from(BACKOFFICE_USER_DASHBOARD_LAYOUT)
        .where(BACKOFFICE_USER_DASHBOARD_LAYOUT.BACKOFFICE_USER_ID.eq(backofficeUserId))
        .fetchOptional()
        .map(r -> r.value1().data());
  }

  public void upsertLayout(UUID backofficeUserId, String layoutJson) {
    dsl.insertInto(BACKOFFICE_USER_DASHBOARD_LAYOUT)
        .set(BACKOFFICE_USER_DASHBOARD_LAYOUT.BACKOFFICE_USER_ID, backofficeUserId)
        .set(BACKOFFICE_USER_DASHBOARD_LAYOUT.LAYOUT, JSONB.valueOf(layoutJson))
        .set(BACKOFFICE_USER_DASHBOARD_LAYOUT.UPDATED_AT, LocalDateTime.now())
        .onConflict(BACKOFFICE_USER_DASHBOARD_LAYOUT.BACKOFFICE_USER_ID)
        .doUpdate()
        .set(BACKOFFICE_USER_DASHBOARD_LAYOUT.LAYOUT, JSONB.valueOf(layoutJson))
        .set(BACKOFFICE_USER_DASHBOARD_LAYOUT.UPDATED_AT, LocalDateTime.now())
        .execute();
  }

  /** Currently active snoozes (snoozed_until in the future): item_key -> snoozed_until. */
  public Map<String, LocalDateTime> activeSnoozes(UUID backofficeUserId) {
    Map<String, LocalDateTime> result = new LinkedHashMap<>();
    dsl.select(BACKOFFICE_ACTION_ITEM_SNOOZE.ITEM_KEY, BACKOFFICE_ACTION_ITEM_SNOOZE.SNOOZED_UNTIL)
        .from(BACKOFFICE_ACTION_ITEM_SNOOZE)
        .where(
            BACKOFFICE_ACTION_ITEM_SNOOZE
                .BACKOFFICE_USER_ID
                .eq(backofficeUserId)
                .and(BACKOFFICE_ACTION_ITEM_SNOOZE.SNOOZED_UNTIL.gt(LocalDateTime.now())))
        .fetch()
        .forEach(
            r ->
                result.put(
                    r.get(BACKOFFICE_ACTION_ITEM_SNOOZE.ITEM_KEY),
                    r.get(BACKOFFICE_ACTION_ITEM_SNOOZE.SNOOZED_UNTIL)));
    return result;
  }

  public void snooze(UUID backofficeUserId, String itemKey, LocalDateTime snoozedUntil) {
    dsl.insertInto(BACKOFFICE_ACTION_ITEM_SNOOZE)
        .set(BACKOFFICE_ACTION_ITEM_SNOOZE.BACKOFFICE_USER_ID, backofficeUserId)
        .set(BACKOFFICE_ACTION_ITEM_SNOOZE.ITEM_KEY, itemKey)
        .set(BACKOFFICE_ACTION_ITEM_SNOOZE.SNOOZED_UNTIL, snoozedUntil)
        .onConflict(
            BACKOFFICE_ACTION_ITEM_SNOOZE.BACKOFFICE_USER_ID,
            BACKOFFICE_ACTION_ITEM_SNOOZE.ITEM_KEY)
        .doUpdate()
        .set(BACKOFFICE_ACTION_ITEM_SNOOZE.SNOOZED_UNTIL, snoozedUntil)
        .execute();
  }
}
