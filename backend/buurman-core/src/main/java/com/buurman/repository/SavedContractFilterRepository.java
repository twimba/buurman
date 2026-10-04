package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.SAVED_CONTRACT_FILTERS;
import static com.buurman.util.SidGenerator.newSavedContractFilterId;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Repository;

import com.buurman.domain.SavedContractFilter;
import com.buurman.domain.Sid;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.SavedContractFilterRecordMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SavedContractFilterRepository {

  private final DSLContext dsl;
  private final SavedContractFilterRecordMapper mapper;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public List<SavedContractFilter> findByTeamIdAndUserId(UUID teamId, UUID userId) {
    return List.copyOf(
        dsl.selectFrom(SAVED_CONTRACT_FILTERS)
            .where(
                SAVED_CONTRACT_FILTERS
                    .TEAM_ID
                    .eq(teamId)
                    .and(SAVED_CONTRACT_FILTERS.USER_ID.eq(userId))
                    .and(SAVED_CONTRACT_FILTERS.DELETED_AT.isNull()))
            .orderBy(SAVED_CONTRACT_FILTERS.CREATED_AT.desc())
            .fetch()
            .map(record -> mapper.toDomain(record, objectMapper)));
  }

  public SavedContractFilter save(SavedContractFilter filter) {
    try {
      LocalDateTime now = LocalDateTime.now(clock);
      UUID id = UUID.randomUUID();
      Sid identifier = newSavedContractFilterId();
      dsl.insertInto(SAVED_CONTRACT_FILTERS)
          .set(SAVED_CONTRACT_FILTERS.ID, id)
          .set(SAVED_CONTRACT_FILTERS.IDENTIFIER, identifier)
          .set(SAVED_CONTRACT_FILTERS.TEAM_ID, filter.getTeamId())
          .set(SAVED_CONTRACT_FILTERS.USER_ID, filter.getUserId())
          .set(SAVED_CONTRACT_FILTERS.NAME, filter.getName())
          .set(
              SAVED_CONTRACT_FILTERS.CRITERIA,
              JSONB.valueOf(objectMapper.writeValueAsString(filter.getCriteria())))
          .set(SAVED_CONTRACT_FILTERS.CREATED_AT, now)
          .set(SAVED_CONTRACT_FILTERS.UPDATED_AT, now)
          .execute();
      filter.setId(id);
      filter.setIdentifier(Optional.of(identifier));
      filter.setCreatedAt(now.toInstant(UTC));
      filter.setUpdatedAt(now.toInstant(UTC));
      return filter;
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize saved filter criteria", e);
    }
  }

  public Optional<SavedContractFilter> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(SAVED_CONTRACT_FILTERS)
        .where(
            SAVED_CONTRACT_FILTERS
                .IDENTIFIER
                .eq(identifier)
                .and(SAVED_CONTRACT_FILTERS.TEAM_ID.eq(teamId))
                .and(SAVED_CONTRACT_FILTERS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(record -> mapper.toDomain(record, objectMapper));
  }

  /**
   * Deletes only if {@code userId} is the filter's own creator — enforced here, not just in the
   * service, as defense in depth for a personal (not team-shared) record. Both "doesn't exist" and
   * "belongs to someone else" surface identically as {@link NotFoundException}: this is a personal
   * record the caller has no right to know exists, so we leak nothing about it, matching how this
   * codebase treats cross-team lookups elsewhere.
   */
  public void softDeleteByIdentifierAndTeamIdAndUserId(Sid identifier, UUID teamId, UUID userId) {
    LocalDateTime now = LocalDateTime.now(clock);
    int updated =
        dsl.update(SAVED_CONTRACT_FILTERS)
            .set(SAVED_CONTRACT_FILTERS.DELETED_AT, now)
            .where(
                SAVED_CONTRACT_FILTERS
                    .IDENTIFIER
                    .eq(identifier)
                    .and(SAVED_CONTRACT_FILTERS.TEAM_ID.eq(teamId))
                    .and(SAVED_CONTRACT_FILTERS.USER_ID.eq(userId))
                    .and(SAVED_CONTRACT_FILTERS.DELETED_AT.isNull()))
            .execute();
    if (updated == 0) {
      throw new NotFoundException("Saved contract filter not found");
    }
  }
}
