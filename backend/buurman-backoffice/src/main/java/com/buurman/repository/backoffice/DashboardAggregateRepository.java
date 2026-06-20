package com.buurman.repository.backoffice;

import static com.buurman.jooq.generated.Tables.CONTACTS;
import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.IMPERSONATION_SESSIONS;
import static com.buurman.jooq.generated.Tables.NOTIFICATION_OUTBOX;
import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.jooq.generated.Tables.TEAMS;
import static com.buurman.jooq.generated.Tables.TEAM_MEMBERS;
import static com.buurman.jooq.generated.Tables.TEAM_PREFERENCES;
import static com.buurman.jooq.generated.Tables.USERS;
import static org.jooq.impl.DSL.exists;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.min;
import static org.jooq.impl.DSL.selectCount;
import static org.jooq.impl.DSL.selectOne;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import com.buurman.dto.response.backoffice.dashboard.GeoResponse.GeoCountry;
import com.buurman.dto.response.backoffice.dashboard.ProductEntitiesResponse.EntityCount;
import com.buurman.dto.response.backoffice.dashboard.TopTeamsResponse.TopTeam;

import lombok.RequiredArgsConstructor;

/**
 * Heavy cross-team aggregates for the backoffice dashboard. These deliberately span all teams
 * (backoffice is platform-wide) but always exclude demo and soft-deleted teams from KPIs.
 */
@Repository
@RequiredArgsConstructor
public class DashboardAggregateRepository {

  private final DSLContext dsl;

  /** Active = non-demo, not soft-deleted. */
  public long countActiveTeams() {
    return count(
        dsl.selectCount().from(TEAMS).where(TEAMS.DEMO.isFalse().and(TEAMS.DELETED_AT.isNull())));
  }

  public long countActiveTeamsWithProperties() {
    return count(
        dsl.selectCount()
            .from(TEAMS)
            .where(
                TEAMS
                    .DEMO
                    .isFalse()
                    .and(TEAMS.DELETED_AT.isNull())
                    .andExists(
                        selectOne()
                            .from(PROPERTIES)
                            .where(
                                PROPERTIES
                                    .TEAM_ID
                                    .eq(TEAMS.ID)
                                    .and(PROPERTIES.DELETED_AT.isNull())))));
  }

  public long countActiveTeamsWithContracts() {
    return count(
        dsl.selectCount()
            .from(TEAMS)
            .where(
                TEAMS
                    .DEMO
                    .isFalse()
                    .and(TEAMS.DELETED_AT.isNull())
                    .andExists(
                        selectOne()
                            .from(CONTRACTS)
                            .where(
                                CONTRACTS
                                    .TEAM_ID
                                    .eq(TEAMS.ID)
                                    .and(CONTRACTS.DELETED_AT.isNull())))));
  }

  /**
   * Product-growth entities created in the last 7 days and overall (non-deleted). Team-scoped
   * entities exclude demo + soft-deleted teams so the numbers reconcile with the other panels.
   */
  public List<EntityCount> productEntityCounts() {
    LocalDateTime since = LocalDateTime.now().minusDays(7);
    return List.of(
        entityCount(
            "Properties",
            PROPERTIES,
            PROPERTIES.TEAM_ID,
            PROPERTIES.DELETED_AT,
            PROPERTIES.CREATED_AT,
            since),
        entityCount(
            "Contracts",
            CONTRACTS,
            CONTRACTS.TEAM_ID,
            CONTRACTS.DELETED_AT,
            CONTRACTS.CREATED_AT,
            since),
        entityCount(
            "Contacts",
            CONTACTS,
            CONTACTS.TEAM_ID,
            CONTACTS.DELETED_AT,
            CONTACTS.CREATED_AT,
            since),
        userCount(since),
        teamCount(since));
  }

  private EntityCount entityCount(
      String label,
      Table<?> table,
      Field<UUID> teamId,
      Field<LocalDateTime> deletedAt,
      Field<LocalDateTime> createdAt,
      LocalDateTime since) {
    long last7Days =
        count(
            dsl.selectCount()
                .from(table)
                .where(deletedAt.isNull().and(createdAt.ge(since)).and(activeTeam(teamId))));
    long total =
        count(dsl.selectCount().from(table).where(deletedAt.isNull().and(activeTeam(teamId))));
    return new EntityCount(label, last7Days, total);
  }

  /** Non-deleted users who belong to at least one active (non-demo) team — excludes demo users. */
  private EntityCount userCount(LocalDateTime since) {
    Condition realUser =
        exists(
            selectOne()
                .from(TEAM_MEMBERS)
                .join(TEAMS)
                .on(TEAMS.ID.eq(TEAM_MEMBERS.TEAM_ID))
                .where(
                    TEAM_MEMBERS
                        .USER_ID
                        .eq(USERS.ID)
                        .and(TEAM_MEMBERS.DELETED_AT.isNull())
                        .and(TEAMS.DEMO.isFalse())
                        .and(TEAMS.DELETED_AT.isNull())));
    long last7Days =
        count(
            dsl.selectCount()
                .from(USERS)
                .where(USERS.DELETED_AT.isNull().and(USERS.CREATED_AT.ge(since)).and(realUser)));
    long total =
        count(dsl.selectCount().from(USERS).where(USERS.DELETED_AT.isNull().and(realUser)));
    return new EntityCount("Users", last7Days, total);
  }

  /** Active (non-demo, non-deleted) teams. */
  private EntityCount teamCount(LocalDateTime since) {
    long last7Days =
        count(
            dsl.selectCount()
                .from(TEAMS)
                .where(
                    TEAMS
                        .DEMO
                        .isFalse()
                        .and(TEAMS.DELETED_AT.isNull())
                        .and(TEAMS.CREATED_AT.ge(since))));
    return new EntityCount("Teams", last7Days, countActiveTeams());
  }

  /** Predicate: the row's team is non-demo and not soft-deleted. */
  private Condition activeTeam(Field<UUID> teamId) {
    return exists(
        selectOne()
            .from(TEAMS)
            .where(TEAMS.ID.eq(teamId).and(TEAMS.DEMO.isFalse()).and(TEAMS.DELETED_AT.isNull())));
  }

  /** Top teams ranked by total entity count (properties + contracts + contacts). */
  public List<TopTeam> topTeamsByActivity(int limit) {
    Field<Integer> activity =
        field(
                selectCount()
                    .from(PROPERTIES)
                    .where(PROPERTIES.TEAM_ID.eq(TEAMS.ID).and(PROPERTIES.DELETED_AT.isNull())))
            .plus(
                field(
                    selectCount()
                        .from(CONTRACTS)
                        .where(CONTRACTS.TEAM_ID.eq(TEAMS.ID).and(CONTRACTS.DELETED_AT.isNull()))))
            .plus(
                field(
                    selectCount()
                        .from(CONTACTS)
                        .where(CONTACTS.TEAM_ID.eq(TEAMS.ID).and(CONTACTS.DELETED_AT.isNull()))));
    return dsl.select(TEAMS.IDENTIFIER, TEAMS.NAME, activity.as("activity"))
        .from(TEAMS)
        .where(TEAMS.DEMO.isFalse().and(TEAMS.DELETED_AT.isNull()))
        .orderBy(field("activity").desc())
        .limit(limit)
        .fetch()
        .map(
            r -> {
              String identifier = r.get(TEAMS.IDENTIFIER).value();
              long score = r.get("activity", Long.class);
              return new TopTeam(identifier, r.get(TEAMS.NAME), score, "/teams/" + identifier);
            });
  }

  /** Retryable outbox backlog (pending/failed, retries remaining). */
  public long outboxBacklog() {
    return count(
        dsl.selectCount()
            .from(NOTIFICATION_OUTBOX)
            .where(
                NOTIFICATION_OUTBOX
                    .STATUS
                    .in("PENDING", "FAILED")
                    .and(NOTIFICATION_OUTBOX.RETRY_COUNT.lt(NOTIFICATION_OUTBOX.MAX_RETRIES))));
  }

  /** Dead-lettered outbox messages (failed, retries exhausted). */
  public long outboxDeadLetter() {
    return count(
        dsl.selectCount()
            .from(NOTIFICATION_OUTBOX)
            .where(
                NOTIFICATION_OUTBOX
                    .STATUS
                    .eq("FAILED")
                    .and(NOTIFICATION_OUTBOX.RETRY_COUNT.ge(NOTIFICATION_OUTBOX.MAX_RETRIES))));
  }

  public Optional<LocalDateTime> oldestBacklogCreatedAt() {
    return oldestCreatedAt(
        NOTIFICATION_OUTBOX
            .STATUS
            .in("PENDING", "FAILED")
            .and(NOTIFICATION_OUTBOX.RETRY_COUNT.lt(NOTIFICATION_OUTBOX.MAX_RETRIES)));
  }

  public Optional<LocalDateTime> oldestDeadLetterCreatedAt() {
    return oldestCreatedAt(
        NOTIFICATION_OUTBOX
            .STATUS
            .eq("FAILED")
            .and(NOTIFICATION_OUTBOX.RETRY_COUNT.ge(NOTIFICATION_OUTBOX.MAX_RETRIES)));
  }

  private Optional<LocalDateTime> oldestCreatedAt(Condition where) {
    return dsl.select(min(NOTIFICATION_OUTBOX.CREATED_AT))
        .from(NOTIFICATION_OUTBOX)
        .where(where)
        .fetchOptional()
        .map(Record1::value1);
  }

  /**
   * Active (non-demo, non-deleted) teams grouped by their default country code, highest first.
   * Excludes demo teams so the breakdown reconciles with the active-teams pillar and the funnel.
   */
  public List<GeoCountry> teamsByCountry() {
    return dsl.select(TEAM_PREFERENCES.DEFAULT_COUNTRY_CODE, DSL.count())
        .from(TEAM_PREFERENCES)
        .join(TEAMS)
        .on(TEAMS.ID.eq(TEAM_PREFERENCES.TEAM_ID))
        .where(
            TEAMS
                .DEMO
                .isFalse()
                .and(TEAMS.DELETED_AT.isNull())
                .and(TEAM_PREFERENCES.DEFAULT_COUNTRY_CODE.isNotNull())
                .and(TEAM_PREFERENCES.DEFAULT_COUNTRY_CODE.ne("")))
        .groupBy(TEAM_PREFERENCES.DEFAULT_COUNTRY_CODE)
        .orderBy(DSL.count().desc())
        .fetch()
        .map(
            r ->
                new GeoCountry(r.get(TEAM_PREFERENCES.DEFAULT_COUNTRY_CODE), r.get(1, Long.class)));
  }

  /** Active (non-demo, non-deleted) teams with no country set (no prefs row, or null/blank). */
  public long teamsWithoutCountry() {
    return count(
        dsl.selectCount()
            .from(TEAMS)
            .leftJoin(TEAM_PREFERENCES)
            .on(TEAM_PREFERENCES.TEAM_ID.eq(TEAMS.ID))
            .where(
                TEAMS
                    .DEMO
                    .isFalse()
                    .and(TEAMS.DELETED_AT.isNull())
                    .and(
                        TEAM_PREFERENCES
                            .DEFAULT_COUNTRY_CODE
                            .isNull()
                            .or(TEAM_PREFERENCES.DEFAULT_COUNTRY_CODE.eq("")))));
  }

  /** Non-deleted properties of active (non-demo) teams grouped by country code, highest first. */
  public List<GeoCountry> propertiesByCountry() {
    return dsl.select(PROPERTIES.COUNTRY_CODE, DSL.count())
        .from(PROPERTIES)
        .join(TEAMS)
        .on(TEAMS.ID.eq(PROPERTIES.TEAM_ID))
        .where(
            PROPERTIES
                .DELETED_AT
                .isNull()
                .and(TEAMS.DELETED_AT.isNull())
                .and(TEAMS.DEMO.isFalse())
                .and(PROPERTIES.COUNTRY_CODE.isNotNull())
                .and(PROPERTIES.COUNTRY_CODE.ne("")))
        .groupBy(PROPERTIES.COUNTRY_CODE)
        .orderBy(DSL.count().desc())
        .fetch()
        .map(r -> new GeoCountry(r.get(PROPERTIES.COUNTRY_CODE), r.get(1, Long.class)));
  }

  /** Geocoded coordinates ({lat, lng}) of non-deleted properties of active teams, capped. */
  public List<double[]> propertyCoordinates(int limit) {
    return dsl.select(PROPERTIES.LATITUDE, PROPERTIES.LONGITUDE)
        .from(PROPERTIES)
        .join(TEAMS)
        .on(TEAMS.ID.eq(PROPERTIES.TEAM_ID))
        .where(
            PROPERTIES
                .DELETED_AT
                .isNull()
                .and(TEAMS.DELETED_AT.isNull())
                .and(TEAMS.DEMO.isFalse())
                .and(PROPERTIES.LATITUDE.isNotNull())
                .and(PROPERTIES.LONGITUDE.isNotNull()))
        .limit(limit)
        .fetch()
        .map(
            r ->
                new double[] {
                  r.get(PROPERTIES.LATITUDE).doubleValue(),
                  r.get(PROPERTIES.LONGITUDE).doubleValue()
                });
  }

  /** Active impersonation sessions activated before {@code threshold} (running too long). */
  public long countLongRunningImpersonations(LocalDateTime threshold) {
    return count(
        dsl.selectCount()
            .from(IMPERSONATION_SESSIONS)
            .where(
                IMPERSONATION_SESSIONS
                    .STATUS
                    .eq("ACTIVE")
                    .and(IMPERSONATION_SESSIONS.ENDED_AT.isNull())
                    .and(IMPERSONATION_SESSIONS.ACTIVATED_AT.lt(threshold))));
  }

  private static long count(org.jooq.SelectConditionStep<?> query) {
    Long result = query.fetchOne(0, Long.class);
    return result != null ? result : 0L;
  }
}
