package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.TEAMS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import com.buurman.domain.Sid;
import com.buurman.domain.Team;
import com.buurman.dto.request.PageRequest;
import com.buurman.exception.NotFoundException;
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
    return dsl.selectFrom(TEAMS).where(TEAMS.ID.eq(id)).fetchOptional().flatMap(mapper::toDomain);
  }

  public Team getById(UUID id) {
    return findById(id).orElseThrow(() -> new NotFoundException("Team not found"));
  }

  public Team getByIdentifier(Sid identifier) {
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
          .set(TEAMS.IDENTIFIER, team.getIdentifier().orElseThrow())
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
          .set(TEAMS.IDENTIFIER, team.getIdentifier().orElseThrow())
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

  public Optional<Team> findByIdentifier(Sid identifier) {
    return dsl.selectFrom(TEAMS)
        .where(TEAMS.IDENTIFIER.eq(identifier))
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  /**
   * Platform-wide paginated query for backoffice. No team_id filter. Only returns teams where
   * deleted_at IS NULL.
   */
  public PaginatedResult<Team> findAllPaginated(PageRequest pageRequest, @Nullable String search) {
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
        r ->
            mapper
                .toDomain(r)
                .orElseThrow(() -> new IllegalStateException("Failed to map team record")));
  }

  /** Find by identifier including soft-deleted teams. For backoffice use. */
  public Optional<Team> findByIdentifierForBackoffice(Sid identifier) {
    return dsl.selectFrom(TEAMS)
        .where(TEAMS.IDENTIFIER.eq(identifier).and(TEAMS.DELETED_AT.isNull()))
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public Team getByIdentifierForBackoffice(Sid identifier) {
    return findByIdentifierForBackoffice(identifier)
        .orElseThrow(() -> new NotFoundException("Team not found"));
  }

  public List<Team> findByIds(Collection<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return List.copyOf(
        dsl.selectFrom(TEAMS)
            .where(TEAMS.ID.in(ids).and(TEAMS.DELETED_AT.isNull()))
            .fetch()
            .map(
                r ->
                    mapper
                        .toDomain(r)
                        .orElseThrow(
                            () -> new IllegalStateException("Failed to map team record"))));
  }

  public List<UUID> findAllActiveTeamIds() {
    return dsl.select(TEAMS.ID).from(TEAMS).where(TEAMS.DELETED_AT.isNull()).fetch(TEAMS.ID);
  }

  public long countAll() {
    Long result =
        dsl.selectCount().from(TEAMS).where(TEAMS.DELETED_AT.isNull()).fetchOne(0, Long.class);
    return result != null ? result : 0L;
  }
}
