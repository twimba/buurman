package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.TEAMS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONB;
import org.springframework.stereotype.Repository;

import com.buurman.domain.Team;
import com.buurman.domain.TeamSettings;
import com.buurman.dto.request.PageRequest;
import com.buurman.jooq.generated.tables.records.TeamsRecord;
import com.buurman.mapper.TeamRecordMapper;
import com.buurman.util.PaginationHelper;
import com.buurman.util.PaginationHelper.PaginatedResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
@RequiredArgsConstructor
public class TeamRepository {

  private final DSLContext dsl;
  private final TeamRecordMapper mapper;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public Optional<Team> findById(UUID id) {
    return dsl.selectFrom(TEAMS).where(TEAMS.ID.eq(id)).fetchOptional().map(mapper::toDomain);
  }

  public Team save(Team team) {
    LocalDateTime now = LocalDateTime.now(clock);

    // Serialize settings to JSONB
    JSONB settingsJsonb = serializeSettings(team.getSettings());

    if (team.getId() == null) {
      // INSERT
      UUID newId = UUID.randomUUID();
      LocalDateTime createdAt =
          team.getCreatedAt() != null ? LocalDateTime.ofInstant(team.getCreatedAt(), UTC) : now;
      LocalDateTime updatedAt =
          team.getUpdatedAt() != null ? LocalDateTime.ofInstant(team.getUpdatedAt(), UTC) : now;

      dsl.insertInto(TEAMS)
          .set(TEAMS.ID, newId)
          .set(TEAMS.IDENTIFIER, team.getIdentifier())
          .set(TEAMS.NAME, team.getName())
          .set(TEAMS.SETTINGS, settingsJsonb)
          .set(TEAMS.CREATED_AT, createdAt)
          .set(TEAMS.UPDATED_AT, updatedAt)
          .set(TEAMS.CREATED_BY, team.getCreatedBy())
          .execute();

      team.setId(newId);
      team.setCreatedAt(createdAt.toInstant(UTC));
      team.setUpdatedAt(updatedAt.toInstant(UTC));
    } else {
      // UPDATE
      LocalDateTime updatedAt =
          team.getUpdatedAt() != null ? LocalDateTime.ofInstant(team.getUpdatedAt(), UTC) : now;

      dsl.update(TEAMS)
          .set(TEAMS.IDENTIFIER, team.getIdentifier())
          .set(TEAMS.NAME, team.getName())
          .set(TEAMS.SETTINGS, settingsJsonb)
          .set(TEAMS.UPDATED_AT, updatedAt)
          .set(TEAMS.UPDATED_BY, team.getUpdatedBy())
          .where(TEAMS.ID.eq(team.getId()))
          .execute();

      team.setUpdatedAt(updatedAt.toInstant(UTC));
    }

    return team;
  }

  public List<Team> findAllWithAutoGenerationEnabled() {
    // Query teams where settings->payments->autoGenerationEnabled = true
    return dsl.selectFrom(TEAMS)
        .where("settings->'payments'->>'autoGenerationEnabled' = 'true'")
        .fetch()
        .map(mapper::toDomain);
  }

  private JSONB serializeSettings(TeamSettings settings) {
    if (settings == null) {
      settings = new TeamSettings(); // Use defaults
    }
    try {
      String json = objectMapper.writeValueAsString(settings);
      return JSONB.valueOf(json);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize team settings", e);
      return JSONB.valueOf("{}");
    }
  }

  public void softDeleteById(UUID id) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(TEAMS).set(TEAMS.DELETED_AT, now).where(TEAMS.ID.eq(id)).execute();
  }

  public Optional<Team> findByIdentifier(String identifier) {
    return dsl.selectFrom(TEAMS)
        .where(TEAMS.IDENTIFIER.eq(identifier))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  /**
   * Platform-wide paginated query for backoffice. No team_id filter. Only returns teams where
   * deleted_at IS NULL.
   */
  public PaginatedResult<Team> findAllPaginated(PageRequest pageRequest, String search) {
    Condition condition = TEAMS.DELETED_AT.isNull();
    if (search != null && !search.isBlank()) {
      condition = condition.and(TEAMS.NAME.likeIgnoreCase("%" + search + "%"));
    }
    Map<String, Field<?>> sortableFields =
        Map.of(
            "name", TEAMS.NAME,
            "createdAt", TEAMS.CREATED_AT,
            "updatedAt", TEAMS.UPDATED_AT);
    return PaginationHelper.paginate(
        dsl,
        TEAMS,
        condition,
        sortableFields,
        TEAMS.CREATED_AT,
        pageRequest,
        r -> mapper.toDomain((TeamsRecord) r));
  }

  /** Find by identifier including soft-deleted teams. For backoffice use. */
  public Optional<Team> findByIdentifierForBackoffice(String identifier) {
    return dsl.selectFrom(TEAMS)
        .where(TEAMS.IDENTIFIER.eq(identifier).and(TEAMS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public long countAll() {
    return dsl.selectCount().from(TEAMS).where(TEAMS.DELETED_AT.isNull()).fetchOne(0, long.class);
  }
}
