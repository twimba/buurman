package com.buurman.repository.backoffice;

import static com.buurman.jooq.generated.Tables.CONTACTS;
import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.DOCUMENTS;
import static com.buurman.jooq.generated.Tables.IMPERSONATION_SESSIONS;
import static com.buurman.jooq.generated.Tables.NOTIFICATION_OUTBOX;
import static com.buurman.jooq.generated.Tables.PHOTOS;
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

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import com.buurman.dto.response.backoffice.dashboard.CountryStats;
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
  private final Clock clock;

  /** Active = non-demo, not soft-deleted. */
  public long countActiveTeams() {
    return count(
        dsl.selectCount().from(TEAMS).where(TEAMS.DEMO.isFalse().and(TEAMS.DELETED_AT.isNull())));
  }

  /**
   * Counts active (non-demo, non-deleted) teams that satisfy every supplied activation milestone.
   * Passing milestones cumulatively (e.g. {@code hasProperty()}, then {@code hasProperty(),
   * hasContact()}) yields a strictly monotonic funnel: each stage is a subset of the previous one,
   * so step-conversion and drop-off are always well-defined.
   */
  public long countActiveTeamsMatching(Condition... milestones) {
    Condition where = TEAMS.DEMO.isFalse().and(TEAMS.DELETED_AT.isNull());
    for (Condition milestone : milestones) {
      where = where.and(milestone);
    }
    return count(dsl.selectCount().from(TEAMS).where(where));
  }

  /** Activation milestone: the team has created at least one (non-deleted) property. */
  public static Condition hasProperty() {
    return exists(
        selectOne()
            .from(PROPERTIES)
            .where(PROPERTIES.TEAM_ID.eq(TEAMS.ID).and(PROPERTIES.DELETED_AT.isNull())));
  }

  /** Activation milestone: the team has added at least one (non-deleted) contact. */
  public static Condition hasContact() {
    return exists(
        selectOne()
            .from(CONTACTS)
            .where(CONTACTS.TEAM_ID.eq(TEAMS.ID).and(CONTACTS.DELETED_AT.isNull())));
  }

  /** Activation milestone: the team has at least one (non-deleted) contract. */
  public static Condition hasContract() {
    return exists(
        selectOne()
            .from(CONTRACTS)
            .where(CONTRACTS.TEAM_ID.eq(TEAMS.ID).and(CONTRACTS.DELETED_AT.isNull())));
  }

  /** Activation milestone: the team has uploaded at least one (non-deleted) document. */
  public static Condition hasDocument() {
    return exists(
        selectOne()
            .from(DOCUMENTS)
            .where(DOCUMENTS.TEAM_ID.eq(TEAMS.ID).and(DOCUMENTS.DELETED_AT.isNull())));
  }

  /**
   * Product-growth entities created in the last 7 days and overall (non-deleted). Team-scoped
   * entities exclude demo + soft-deleted teams so the numbers reconcile with the other panels.
   */
  public List<EntityCount> productEntityCounts() {
    LocalDateTime since = LocalDateTime.now(clock).minusDays(7);
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
        uploadsCount(since),
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
    return new EntityCount(
        label,
        activeRowCount(table, teamId, deletedAt, createdAt, since),
        activeRowTotal(table, teamId, deletedAt));
  }

  /** Uploads = documents + photos (both keyed by uploaded_at), across active teams. */
  private EntityCount uploadsCount(LocalDateTime since) {
    long last7Days =
        activeRowCount(
                DOCUMENTS, DOCUMENTS.TEAM_ID, DOCUMENTS.DELETED_AT, DOCUMENTS.UPLOADED_AT, since)
            + activeRowCount(PHOTOS, PHOTOS.TEAM_ID, PHOTOS.DELETED_AT, PHOTOS.UPLOADED_AT, since);
    long total =
        activeRowTotal(DOCUMENTS, DOCUMENTS.TEAM_ID, DOCUMENTS.DELETED_AT)
            + activeRowTotal(PHOTOS, PHOTOS.TEAM_ID, PHOTOS.DELETED_AT);
    return new EntityCount("Uploads", last7Days, total);
  }

  private long activeRowCount(
      Table<?> table,
      Field<UUID> teamId,
      Field<LocalDateTime> deletedAt,
      Field<LocalDateTime> createdAt,
      LocalDateTime since) {
    return count(
        dsl.selectCount()
            .from(table)
            .where(deletedAt.isNull().and(createdAt.ge(since)).and(activeTeam(teamId))));
  }

  private long activeRowTotal(Table<?> table, Field<UUID> teamId, Field<LocalDateTime> deletedAt) {
    return count(dsl.selectCount().from(table).where(deletedAt.isNull().and(activeTeam(teamId))));
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

  /**
   * Per-country rollup grouped by the team's default country: teams, properties, contracts, and the
   * normalized monthly rent of active contracts (EUR cents). Excludes demo + soft-deleted teams.
   */
  public List<CountryStats> countryStats() {
    // [teams, properties, contracts, monthlyValueMinor]
    Map<String, long[]> acc = new LinkedHashMap<>();

    dsl.select(TEAM_PREFERENCES.DEFAULT_COUNTRY_CODE, DSL.count())
        .from(TEAM_PREFERENCES)
        .join(TEAMS)
        .on(TEAMS.ID.eq(TEAM_PREFERENCES.TEAM_ID))
        .where(activeCountryTeam())
        .groupBy(TEAM_PREFERENCES.DEFAULT_COUNTRY_CODE)
        .fetch()
        .forEach(r -> slot(acc, r.value1())[0] = r.get(1, Long.class));

    dsl.select(TEAM_PREFERENCES.DEFAULT_COUNTRY_CODE, DSL.count())
        .from(PROPERTIES)
        .join(TEAMS)
        .on(TEAMS.ID.eq(PROPERTIES.TEAM_ID))
        .join(TEAM_PREFERENCES)
        .on(TEAM_PREFERENCES.TEAM_ID.eq(TEAMS.ID))
        .where(PROPERTIES.DELETED_AT.isNull().and(activeCountryTeam()))
        .groupBy(TEAM_PREFERENCES.DEFAULT_COUNTRY_CODE)
        .fetch()
        .forEach(r -> slot(acc, r.value1())[1] = r.get(1, Long.class));

    Field<java.math.BigDecimal> monthlyActive =
        DSL.when(
                CONTRACTS.STATUS.eq("ACTIVE"),
                DSL.choose(CONTRACTS.PAYMENT_FREQUENCY)
                    .when("MONTHLY", CONTRACTS.RENT_AMOUNT)
                    .when("QUARTERLY", CONTRACTS.RENT_AMOUNT.divide(3))
                    .when("SEMI_ANNUAL", CONTRACTS.RENT_AMOUNT.divide(6))
                    .when("SEMIANNUAL", CONTRACTS.RENT_AMOUNT.divide(6))
                    .when("ANNUAL", CONTRACTS.RENT_AMOUNT.divide(12))
                    .when("BIENNIAL", CONTRACTS.RENT_AMOUNT.divide(24))
                    .when("TRIENNIAL", CONTRACTS.RENT_AMOUNT.divide(36))
                    .otherwise(CONTRACTS.RENT_AMOUNT))
            .otherwise(java.math.BigDecimal.ZERO);
    dsl.select(TEAM_PREFERENCES.DEFAULT_COUNTRY_CODE, DSL.count(), DSL.sum(monthlyActive))
        .from(CONTRACTS)
        .join(TEAMS)
        .on(TEAMS.ID.eq(CONTRACTS.TEAM_ID))
        .join(TEAM_PREFERENCES)
        .on(TEAM_PREFERENCES.TEAM_ID.eq(TEAMS.ID))
        .where(CONTRACTS.DELETED_AT.isNull().and(activeCountryTeam()))
        .groupBy(TEAM_PREFERENCES.DEFAULT_COUNTRY_CODE)
        .fetch()
        .forEach(
            r -> {
              long[] s = slot(acc, r.value1());
              s[2] = r.get(1, Long.class);
              java.math.BigDecimal sum = r.get(2, java.math.BigDecimal.class);
              s[3] = sum == null ? 0 : Math.round(sum.doubleValue() * 100);
            });

    return acc.entrySet().stream()
        .map(
            e ->
                new CountryStats(
                    e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2], e.getValue()[3]))
        .sorted(java.util.Comparator.comparingLong(CountryStats::teams).reversed())
        .toList();
  }

  private static Condition activeCountryTeam() {
    return TEAMS
        .DEMO
        .isFalse()
        .and(TEAMS.DELETED_AT.isNull())
        .and(TEAM_PREFERENCES.DEFAULT_COUNTRY_CODE.isNotNull())
        .and(TEAM_PREFERENCES.DEFAULT_COUNTRY_CODE.ne(""));
  }

  private static long[] slot(Map<String, long[]> acc, String code) {
    return acc.computeIfAbsent(code, k -> new long[4]);
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
