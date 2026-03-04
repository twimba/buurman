package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.TEAM_PREFERENCES;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.TeamPreferences;
import com.buurman.jooq.generated.tables.records.TeamPreferencesRecord;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TeamPreferencesRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public Optional<TeamPreferences> findByTeamId(UUID teamId) {
    return dsl.selectFrom(TEAM_PREFERENCES)
        .where(TEAM_PREFERENCES.TEAM_ID.eq(teamId))
        .fetchOptional()
        .map(this::toDomain);
  }

  /** Returns preferences for the team, creating a default row if none exists. */
  public TeamPreferences getByTeamId(UUID teamId) {
    return findByTeamId(teamId).orElseGet(() -> createDefaults(teamId));
  }

  public TeamPreferences save(TeamPreferences prefs) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (prefs.getId() == null) {
      UUID newId = UUID.randomUUID();
      LocalDateTime createdAt = now;

      dsl.insertInto(TEAM_PREFERENCES)
          .set(TEAM_PREFERENCES.ID, newId)
          .set(TEAM_PREFERENCES.TEAM_ID, prefs.getTeamId())
          .set(TEAM_PREFERENCES.PAYMENTS_AHEAD_COUNT, prefs.getPaymentsAheadCount())
          .set(TEAM_PREFERENCES.AUTO_GENERATION_ENABLED, prefs.isAutoGenerationEnabled())
          .set(TEAM_PREFERENCES.DEFAULT_CURRENCY, prefs.getDefaultCurrency().orElse(null))
          .set(TEAM_PREFERENCES.DEFAULT_COUNTRY, prefs.getDefaultCountry())
          .set(TEAM_PREFERENCES.TIMEZONE, prefs.getTimezone())
          .set(TEAM_PREFERENCES.DATE_FORMAT, prefs.getDateFormat())
          .set(TEAM_PREFERENCES.FISCAL_YEAR_START_MONTH, prefs.getFiscalYearStartMonth())
          .set(TEAM_PREFERENCES.TAKEOUT_RETENTION_DAYS, prefs.getTakeoutRetentionDays())
          .set(TEAM_PREFERENCES.CREATED_AT, createdAt)
          .set(TEAM_PREFERENCES.UPDATED_AT, now)
          .execute();
      prefs.setId(newId);
      prefs.setCreatedAt(createdAt.toInstant(UTC));
      prefs.setUpdatedAt(now.toInstant(UTC));
    } else {
      dsl.update(TEAM_PREFERENCES)
          .set(TEAM_PREFERENCES.PAYMENTS_AHEAD_COUNT, prefs.getPaymentsAheadCount())
          .set(TEAM_PREFERENCES.AUTO_GENERATION_ENABLED, prefs.isAutoGenerationEnabled())
          .set(TEAM_PREFERENCES.DEFAULT_CURRENCY, prefs.getDefaultCurrency().orElse(null))
          .set(TEAM_PREFERENCES.DEFAULT_COUNTRY, prefs.getDefaultCountry())
          .set(TEAM_PREFERENCES.TIMEZONE, prefs.getTimezone())
          .set(TEAM_PREFERENCES.DATE_FORMAT, prefs.getDateFormat())
          .set(TEAM_PREFERENCES.FISCAL_YEAR_START_MONTH, prefs.getFiscalYearStartMonth())
          .set(TEAM_PREFERENCES.TAKEOUT_RETENTION_DAYS, prefs.getTakeoutRetentionDays())
          .set(TEAM_PREFERENCES.UPDATED_AT, now)
          .where(TEAM_PREFERENCES.ID.eq(prefs.getId()))
          .execute();
      prefs.setUpdatedAt(now.toInstant(UTC));
    }
    return prefs;
  }

  /** Find all team IDs where auto payment generation is enabled. */
  public List<UUID> findTeamIdsWithAutoGenerationEnabled() {
    return List.copyOf(
        dsl.select(TEAM_PREFERENCES.TEAM_ID)
            .from(TEAM_PREFERENCES)
            .where(TEAM_PREFERENCES.AUTO_GENERATION_ENABLED.isTrue())
            .fetch(TEAM_PREFERENCES.TEAM_ID));
  }

  private TeamPreferences createDefaults(UUID teamId) {
    TeamPreferences prefs = new TeamPreferences();
    prefs.setTeamId(teamId);
    return save(prefs);
  }

  private TeamPreferences toDomain(TeamPreferencesRecord record) {
    TeamPreferences prefs = new TeamPreferences();
    prefs.setId(record.getId());
    prefs.setTeamId(record.getTeamId());
    prefs.setPaymentsAheadCount(record.getPaymentsAheadCount());
    prefs.setAutoGenerationEnabled(record.getAutoGenerationEnabled());
    prefs.setDefaultCurrency(Optional.ofNullable(record.getDefaultCurrency()));
    prefs.setDefaultCountry(record.getDefaultCountry());
    prefs.setTimezone(record.getTimezone());
    prefs.setDateFormat(record.getDateFormat());
    prefs.setFiscalYearStartMonth(record.getFiscalYearStartMonth());
    prefs.setTakeoutRetentionDays(record.getTakeoutRetentionDays());
    prefs.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    prefs.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    return prefs;
  }
}
