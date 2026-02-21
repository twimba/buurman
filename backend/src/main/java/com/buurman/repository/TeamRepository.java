package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.TEAMS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.springframework.stereotype.Repository;

import com.buurman.domain.Team;
import com.buurman.dto.request.PageRequest;
import com.buurman.exception.NotFoundException;
import com.buurman.jooq.generated.tables.records.TeamsRecord;
import com.buurman.mapper.TeamRecordMapper;
import com.buurman.util.PaginationHelper;
import com.buurman.util.PaginationHelper.PaginatedResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
@RequiredArgsConstructor
public class TeamRepository {

  private final DSLContext dsl;
  private final TeamRecordMapper mapper;
  private final Clock clock;

  public Optional<Team> findById(UUID id) {
    return dsl.selectFrom(TEAMS).where(TEAMS.ID.eq(id)).fetchOptional().map(mapper::toDomain);
  }

  public Team getById(UUID id) {
    return findById(id).orElseThrow(() -> new NotFoundException("Team not found"));
  }

  public Team getByIdentifier(String identifier) {
    return findByIdentifier(identifier).orElseThrow(() -> new NotFoundException("Team not found"));
  }

  public Team save(Team team) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (team.getId() == null) {
      UUID newId = UUID.randomUUID();
      LocalDateTime createdAt =
          team.getCreatedAt() != null ? LocalDateTime.ofInstant(team.getCreatedAt(), UTC) : now;
      LocalDateTime updatedAt =
          team.getUpdatedAt() != null ? LocalDateTime.ofInstant(team.getUpdatedAt(), UTC) : now;

      dsl.insertInto(TEAMS)
          .set(TEAMS.ID, newId)
          .set(TEAMS.IDENTIFIER, team.getIdentifier())
          .set(TEAMS.NAME, team.getName())
          .set(TEAMS.DEMO, team.isDemo())
          .set(TEAMS.CREATED_AT, createdAt)
          .set(TEAMS.UPDATED_AT, updatedAt)
          .set(TEAMS.CREATED_BY, team.getCreatedBy())
          .execute();

      team.setId(newId);
      team.setCreatedAt(createdAt.toInstant(UTC));
      team.setUpdatedAt(updatedAt.toInstant(UTC));
    } else {
      LocalDateTime updatedAt =
          team.getUpdatedAt() != null ? LocalDateTime.ofInstant(team.getUpdatedAt(), UTC) : now;

      dsl.update(TEAMS)
          .set(TEAMS.IDENTIFIER, team.getIdentifier())
          .set(TEAMS.NAME, team.getName())
          .set(TEAMS.UPDATED_AT, updatedAt)
          .set(TEAMS.UPDATED_BY, team.getUpdatedBy())
          .where(TEAMS.ID.eq(team.getId()))
          .execute();

      team.setUpdatedAt(updatedAt.toInstant(UTC));
    }

    return team;
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

  public Team getByIdentifierForBackoffice(String identifier) {
    return findByIdentifierForBackoffice(identifier)
        .orElseThrow(() -> new NotFoundException("Team not found"));
  }

  public long countAll() {
    return dsl.selectCount().from(TEAMS).where(TEAMS.DELETED_AT.isNull()).fetchOne(0, long.class);
  }
}
