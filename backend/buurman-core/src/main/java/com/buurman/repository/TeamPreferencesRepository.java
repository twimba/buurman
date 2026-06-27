package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.TEAMS;
import static com.buurman.jooq.generated.Tables.TEAM_PREFERENCES;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Repository;

import com.buurman.domain.TeamPreferences;
import com.buurman.jooq.generated.tables.records.TeamPreferencesRecord;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TeamPreferencesRepository {

  // TODO: Replace with TEAM_PREFERENCES.DEFAULT_LANGUAGE after running JOOQ codegen:
  //       cd backend && mvn generate-sources -pl buurman-jooq -am
  private static final Field<String> DEFAULT_LANGUAGE =
      DSL.field(DSL.name("default_language"), SQLDataType.VARCHAR);

  private final DSLContext dsl;
  private final Clock clock;

  public Optional<TeamPreferences> findByTeamId(UUID teamId) {
    return dsl.selectFrom(TEAM_PREFERENCES)
        .where(TEAM_PREFERENCES.TEAM_ID.eq(teamId))
        .fetchOptional()
        .map(this::toDomain);
  }

  /**
   * Returns the team's saved preferences, or in-memory defaults if none have been persisted yet.
   * Reads never write: a row is created only when a caller explicitly {@link #save}s (e.g. updating
   * settings). This keeps the method safe inside read-only transactions.
   */
  public TeamPreferences getByTeamId(UUID teamId) {
    return findByTeamId(teamId).orElseGet(() -> defaultPreferences(teamId));
  }

  public TeamPreferences save(TeamPreferences prefs) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (prefs.getId() == null) {
      UUID newId = UUID.randomUUID();

      dsl.insertInto(TEAM_PREFERENCES)
          .set(TEAM_PREFERENCES.ID, newId)
          .set(TEAM_PREFERENCES.TEAM_ID, prefs.getTeamId())
          .set(TEAM_PREFERENCES.PAYMENTS_AHEAD_COUNT, prefs.getPaymentsAheadCount())
          .set(TEAM_PREFERENCES.AUTO_GENERATION_ENABLED, prefs.isAutoGenerationEnabled())
          .set(TEAM_PREFERENCES.DEFAULT_CURRENCY, prefs.getDefaultCurrency())
          .set(TEAM_PREFERENCES.DEFAULT_COUNTRY_CODE, prefs.getDefaultCountryCode())
          .set(TEAM_PREFERENCES.TIMEZONE, prefs.getTimezone())
          .set(TEAM_PREFERENCES.DATE_FORMAT, prefs.getDateFormat())
          .set(TEAM_PREFERENCES.FISCAL_YEAR_START_MONTH, prefs.getFiscalYearStartMonth())
          .set(DEFAULT_LANGUAGE, prefs.getDefaultLanguage())
          .set(TEAM_PREFERENCES.TAKEOUT_RETENTION_DAYS, prefs.getTakeoutRetentionDays())
          .set(
              TEAM_PREFERENCES.ONBOARDING_COMPLETED_AT,
              prefs
                  .getOnboardingCompletedAt()
                  .map(i -> LocalDateTime.ofInstant(i, UTC))
                  .orElse(null))
          .set(TEAM_PREFERENCES.CREATED_AT, now)
          .set(TEAM_PREFERENCES.UPDATED_AT, now)
          .execute();
      prefs.setId(newId);
      prefs.setCreatedAt(now.toInstant(UTC));
      prefs.setUpdatedAt(now.toInstant(UTC));
    } else {
      dsl.update(TEAM_PREFERENCES)
          .set(TEAM_PREFERENCES.PAYMENTS_AHEAD_COUNT, prefs.getPaymentsAheadCount())
          .set(TEAM_PREFERENCES.AUTO_GENERATION_ENABLED, prefs.isAutoGenerationEnabled())
          .set(TEAM_PREFERENCES.DEFAULT_CURRENCY, prefs.getDefaultCurrency())
          .set(TEAM_PREFERENCES.DEFAULT_COUNTRY_CODE, prefs.getDefaultCountryCode())
          .set(TEAM_PREFERENCES.TIMEZONE, prefs.getTimezone())
          .set(TEAM_PREFERENCES.DATE_FORMAT, prefs.getDateFormat())
          .set(TEAM_PREFERENCES.FISCAL_YEAR_START_MONTH, prefs.getFiscalYearStartMonth())
          .set(DEFAULT_LANGUAGE, prefs.getDefaultLanguage())
          .set(TEAM_PREFERENCES.TAKEOUT_RETENTION_DAYS, prefs.getTakeoutRetentionDays())
          .set(
              TEAM_PREFERENCES.ONBOARDING_COMPLETED_AT,
              prefs
                  .getOnboardingCompletedAt()
                  .map(i -> LocalDateTime.ofInstant(i, UTC))
                  .orElse(null))
          .set(TEAM_PREFERENCES.UPDATED_AT, now)
          .where(TEAM_PREFERENCES.ID.eq(prefs.getId()))
          .execute();
      prefs.setUpdatedAt(now.toInstant(UTC));
    }
    return prefs;
  }

  /** Find all non-demo team IDs where auto payment generation is enabled. */
  public List<UUID> findTeamIdsWithAutoGenerationEnabled() {
    return List.copyOf(
        dsl.select(TEAM_PREFERENCES.TEAM_ID)
            .from(TEAM_PREFERENCES)
            .join(TEAMS)
            .on(TEAMS.ID.eq(TEAM_PREFERENCES.TEAM_ID))
            .where(TEAM_PREFERENCES.AUTO_GENERATION_ENABLED.isTrue())
            .and(TEAMS.DEMO.isFalse())
            .fetch(TEAM_PREFERENCES.TEAM_ID));
  }

  /** Builds default preferences in memory without persisting them (see {@link #getByTeamId}). */
  private TeamPreferences defaultPreferences(UUID teamId) {
    TeamPreferences prefs = new TeamPreferences();
    prefs.setTeamId(teamId);
    return prefs;
  }

  private TeamPreferences toDomain(TeamPreferencesRecord record) {
    TeamPreferences prefs = new TeamPreferences();
    prefs.setId(record.getId());
    prefs.setTeamId(record.getTeamId());
    prefs.setPaymentsAheadCount(record.getPaymentsAheadCount());
    prefs.setAutoGenerationEnabled(record.getAutoGenerationEnabled());
    prefs.setDefaultCurrency(
        record.getDefaultCurrency() != null ? record.getDefaultCurrency() : "EUR");
    prefs.setDefaultCountryCode(record.getDefaultCountryCode());
    prefs.setTimezone(record.getTimezone());
    prefs.setDateFormat(record.getDateFormat());
    prefs.setFiscalYearStartMonth(record.getFiscalYearStartMonth());
    prefs.setDefaultLanguage(Optional.ofNullable(record.get(DEFAULT_LANGUAGE)).orElse("en"));
    prefs.setTakeoutRetentionDays(record.getTakeoutRetentionDays());
    prefs.setOnboardingCompletedAt(
        Optional.ofNullable(record.getOnboardingCompletedAt()).map(ldt -> ldt.toInstant(UTC)));
    prefs.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    prefs.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    return prefs;
  }
}
