package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.DATA_TAKEOUTS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.springframework.stereotype.Repository;

import com.buurman.domain.DataTakeout;
import com.buurman.domain.DataTakeout.TakeoutStatus;
import com.buurman.domain.Ulid;
import com.buurman.dto.request.PageRequest;
import com.buurman.exception.NotFoundException;
import com.buurman.jooq.generated.tables.records.DataTakeoutsRecord;
import com.buurman.util.PaginationHelper;
import com.buurman.util.PaginationHelper.PaginatedResult;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class DataTakeoutRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public DataTakeout save(DataTakeout takeout) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (takeout.getId() == null) {
      UUID id = UUID.randomUUID();
      LocalDateTime createdAt = now;

      dsl.insertInto(DATA_TAKEOUTS)
          .set(DATA_TAKEOUTS.ID, id)
          .set(DATA_TAKEOUTS.IDENTIFIER, takeout.getIdentifier().orElseThrow())
          .set(DATA_TAKEOUTS.TEAM_ID, takeout.getTeamId())
          .set(DATA_TAKEOUTS.STATUS, takeout.getStatus().name())
          .set(DATA_TAKEOUTS.PROGRESS, takeout.getProgress())
          .set(DATA_TAKEOUTS.FILE_KEY, takeout.getFileKey().orElse(null))
          .set(DATA_TAKEOUTS.FILE_SIZE, takeout.getFileSize().orElse(null))
          .set(DATA_TAKEOUTS.ERROR, takeout.getError().orElse(null))
          .set(DATA_TAKEOUTS.CREATED_BY, takeout.getCreatedBy())
          .set(DATA_TAKEOUTS.UPDATED_BY, takeout.getUpdatedBy())
          .set(DATA_TAKEOUTS.CREATED_AT, createdAt)
          .set(DATA_TAKEOUTS.UPDATED_AT, now)
          .set(
              DATA_TAKEOUTS.COMPLETED_AT,
              takeout.getCompletedAt().map(i -> LocalDateTime.ofInstant(i, UTC)).orElse(null))
          .set(
              DATA_TAKEOUTS.EXPIRES_AT,
              takeout.getExpiresAt().map(i -> LocalDateTime.ofInstant(i, UTC)).orElse(null))
          .execute();

      takeout.setId(id);
      takeout.setCreatedAt(createdAt.toInstant(UTC));
      takeout.setUpdatedAt(now.toInstant(UTC));
    } else {
      dsl.update(DATA_TAKEOUTS)
          .set(DATA_TAKEOUTS.STATUS, takeout.getStatus().name())
          .set(DATA_TAKEOUTS.PROGRESS, takeout.getProgress())
          .set(DATA_TAKEOUTS.FILE_KEY, takeout.getFileKey().orElse(null))
          .set(DATA_TAKEOUTS.FILE_SIZE, takeout.getFileSize().orElse(null))
          .set(DATA_TAKEOUTS.ERROR, takeout.getError().orElse(null))
          .set(DATA_TAKEOUTS.UPDATED_BY, takeout.getUpdatedBy())
          .set(DATA_TAKEOUTS.UPDATED_AT, now)
          .set(
              DATA_TAKEOUTS.COMPLETED_AT,
              takeout.getCompletedAt().map(i -> LocalDateTime.ofInstant(i, UTC)).orElse(null))
          .set(
              DATA_TAKEOUTS.EXPIRES_AT,
              takeout.getExpiresAt().map(i -> LocalDateTime.ofInstant(i, UTC)).orElse(null))
          .where(
              DATA_TAKEOUTS
                  .ID
                  .eq(takeout.getId())
                  .and(DATA_TAKEOUTS.TEAM_ID.eq(takeout.getTeamId())))
          .execute();

      takeout.setUpdatedAt(now.toInstant(UTC));
    }

    return takeout;
  }

  public Optional<DataTakeout> findByIdentifierAndTeamId(Ulid identifier, UUID teamId) {
    return dsl.selectFrom(DATA_TAKEOUTS)
        .where(
            DATA_TAKEOUTS
                .IDENTIFIER
                .eq(identifier)
                .and(DATA_TAKEOUTS.TEAM_ID.eq(teamId))
                .and(DATA_TAKEOUTS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(this::toDomain);
  }

  public DataTakeout getByIdentifierAndTeamId(Ulid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Data takeout not found"));
  }

  public Optional<DataTakeout> findById(UUID id) {
    return dsl.selectFrom(DATA_TAKEOUTS)
        .where(DATA_TAKEOUTS.ID.eq(id).and(DATA_TAKEOUTS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(this::toDomain);
  }

  public PaginatedResult<DataTakeout> findAllByTeamId(UUID teamId, PageRequest pageRequest) {
    Condition condition = DATA_TAKEOUTS.TEAM_ID.eq(teamId).and(DATA_TAKEOUTS.DELETED_AT.isNull());
    Map<String, Field<?>> sortableFields =
        Map.of(
            "createdAt", DATA_TAKEOUTS.CREATED_AT,
            "status", DATA_TAKEOUTS.STATUS,
            "completedAt", DATA_TAKEOUTS.COMPLETED_AT);
    return PaginationHelper.paginate(
        dsl,
        DATA_TAKEOUTS,
        condition,
        sortableFields,
        DATA_TAKEOUTS.CREATED_AT,
        pageRequest,
        this::toDomain);
  }

  public void updateProgress(UUID id, int progress) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(DATA_TAKEOUTS)
        .set(DATA_TAKEOUTS.PROGRESS, progress)
        .set(DATA_TAKEOUTS.UPDATED_AT, now)
        .where(DATA_TAKEOUTS.ID.eq(id))
        .execute();
  }

  public void markCompleted(UUID id, String fileKey, long fileSize, Instant expiresAt) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(DATA_TAKEOUTS)
        .set(DATA_TAKEOUTS.STATUS, TakeoutStatus.COMPLETED.name())
        .set(DATA_TAKEOUTS.PROGRESS, 100)
        .set(DATA_TAKEOUTS.FILE_KEY, fileKey)
        .set(DATA_TAKEOUTS.FILE_SIZE, fileSize)
        .set(DATA_TAKEOUTS.COMPLETED_AT, now)
        .set(DATA_TAKEOUTS.EXPIRES_AT, LocalDateTime.ofInstant(expiresAt, UTC))
        .set(DATA_TAKEOUTS.UPDATED_AT, now)
        .where(DATA_TAKEOUTS.ID.eq(id))
        .execute();
  }

  public void markFailed(UUID id, String error) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(DATA_TAKEOUTS)
        .set(DATA_TAKEOUTS.STATUS, TakeoutStatus.FAILED.name())
        .set(DATA_TAKEOUTS.ERROR, error)
        .set(DATA_TAKEOUTS.COMPLETED_AT, now)
        .set(DATA_TAKEOUTS.UPDATED_AT, now)
        .where(DATA_TAKEOUTS.ID.eq(id))
        .execute();
  }

  public boolean hasInProgressByTeamId(UUID teamId) {
    return dsl.fetchExists(
        dsl.selectOne()
            .from(DATA_TAKEOUTS)
            .where(
                DATA_TAKEOUTS
                    .TEAM_ID
                    .eq(teamId)
                    .and(DATA_TAKEOUTS.DELETED_AT.isNull())
                    .and(
                        DATA_TAKEOUTS.STATUS.in(
                            TakeoutStatus.PENDING.name(), TakeoutStatus.PROCESSING.name()))));
  }

  public long countActiveByTeamId(UUID teamId) {
    Long count =
        dsl.selectCount()
            .from(DATA_TAKEOUTS)
            .where(DATA_TAKEOUTS.TEAM_ID.eq(teamId).and(DATA_TAKEOUTS.DELETED_AT.isNull()))
            .fetchOne(0, long.class);
    return count != null ? count : 0L;
  }

  public List<DataTakeout> findExpired(Instant now) {
    LocalDateTime cutoff = LocalDateTime.ofInstant(now, UTC);
    return dsl.selectFrom(DATA_TAKEOUTS)
        .where(
            DATA_TAKEOUTS
                .EXPIRES_AT
                .isNotNull()
                .and(DATA_TAKEOUTS.EXPIRES_AT.le(cutoff))
                .and(DATA_TAKEOUTS.DELETED_AT.isNull()))
        .fetch()
        .map(this::toDomain);
  }

  public void softDelete(UUID id) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(DATA_TAKEOUTS)
        .set(DATA_TAKEOUTS.DELETED_AT, now)
        .set(DATA_TAKEOUTS.UPDATED_AT, now)
        .where(DATA_TAKEOUTS.ID.eq(id))
        .execute();
  }

  private DataTakeout toDomain(DataTakeoutsRecord record) {
    return DataTakeout.builder()
        .id(record.getId())
        .identifier(Optional.of(record.getIdentifier()))
        .teamId(record.getTeamId())
        .status(TakeoutStatus.valueOf(record.getStatus()))
        .progress(record.getProgress())
        .fileKey(Optional.ofNullable(record.getFileKey()))
        .fileSize(Optional.ofNullable(record.getFileSize()))
        .error(Optional.ofNullable(record.getError()))
        .createdBy(record.getCreatedBy())
        .updatedBy(record.getUpdatedBy())
        .createdAt(record.getCreatedAt().toInstant(UTC))
        .updatedAt(record.getUpdatedAt().toInstant(UTC))
        .completedAt(Optional.ofNullable(record.getCompletedAt()).map(t -> t.toInstant(UTC)))
        .expiresAt(Optional.ofNullable(record.getExpiresAt()).map(t -> t.toInstant(UTC)))
        .deletedAt(Optional.ofNullable(record.getDeletedAt()).map(t -> t.toInstant(UTC)))
        .build();
  }
}
