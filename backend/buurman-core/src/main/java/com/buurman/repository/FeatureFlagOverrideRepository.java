package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.FEATURE_FLAG_OVERRIDES;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.FeatureFlagOverride;
import com.buurman.domain.OverrideScope;
import com.buurman.jooq.generated.tables.records.FeatureFlagOverridesRecord;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class FeatureFlagOverrideRepository {

  private final DSLContext dsl;
  private final Clock clock;

  /**
   * Batch-fetch all overrides applicable to a user+team+segments combination. Single query, no N+1.
   */
  public List<FeatureFlagOverride> findApplicableOverrides(
      UUID userId, UUID teamId, List<String> segmentKeys) {
    Condition condition =
        FEATURE_FLAG_OVERRIDES
            .DELETED_AT
            .isNull()
            .and(
                FEATURE_FLAG_OVERRIDES
                    .SCOPE
                    .eq(OverrideScope.USER.toDbValue())
                    .and(FEATURE_FLAG_OVERRIDES.USER_ID.eq(userId))
                    .and(FEATURE_FLAG_OVERRIDES.TEAM_ID.eq(teamId))
                    .or(
                        FEATURE_FLAG_OVERRIDES
                            .SCOPE
                            .eq(OverrideScope.TEAM.toDbValue())
                            .and(FEATURE_FLAG_OVERRIDES.TEAM_ID.eq(teamId))
                            .and(FEATURE_FLAG_OVERRIDES.USER_ID.isNull()))
                    .or(
                        segmentKeys.isEmpty()
                            ? org.jooq.impl.DSL.falseCondition()
                            : FEATURE_FLAG_OVERRIDES
                                .SCOPE
                                .eq(OverrideScope.SEGMENT.toDbValue())
                                .and(FEATURE_FLAG_OVERRIDES.SEGMENT_KEY.in(segmentKeys))));

    return List.copyOf(
        dsl.selectFrom(FEATURE_FLAG_OVERRIDES).where(condition).fetch().map(this::toDomain));
  }

  /** Fetch overrides for team-only evaluation (no user context). */
  public List<FeatureFlagOverride> findTeamOverrides(UUID teamId, List<String> segmentKeys) {
    Condition condition =
        FEATURE_FLAG_OVERRIDES
            .DELETED_AT
            .isNull()
            .and(
                FEATURE_FLAG_OVERRIDES
                    .SCOPE
                    .eq(OverrideScope.TEAM.toDbValue())
                    .and(FEATURE_FLAG_OVERRIDES.TEAM_ID.eq(teamId))
                    .and(FEATURE_FLAG_OVERRIDES.USER_ID.isNull())
                    .or(
                        segmentKeys.isEmpty()
                            ? org.jooq.impl.DSL.falseCondition()
                            : FEATURE_FLAG_OVERRIDES
                                .SCOPE
                                .eq(OverrideScope.SEGMENT.toDbValue())
                                .and(FEATURE_FLAG_OVERRIDES.SEGMENT_KEY.in(segmentKeys))));

    return List.copyOf(
        dsl.selectFrom(FEATURE_FLAG_OVERRIDES).where(condition).fetch().map(this::toDomain));
  }

  /** All overrides for a given flag (backoffice view). */
  public List<FeatureFlagOverride> findByFlagKey(String flagKey) {
    return List.copyOf(
        dsl.selectFrom(FEATURE_FLAG_OVERRIDES)
            .where(
                FEATURE_FLAG_OVERRIDES
                    .FLAG_KEY
                    .eq(flagKey)
                    .and(FEATURE_FLAG_OVERRIDES.DELETED_AT.isNull()))
            .orderBy(FEATURE_FLAG_OVERRIDES.SCOPE.asc(), FEATURE_FLAG_OVERRIDES.PRIORITY.asc())
            .fetch()
            .map(this::toDomain));
  }

  /** Find a specific override by its composite key. */
  public Optional<FeatureFlagOverride> findByScope(
      String flagKey,
      OverrideScope scope,
      Optional<UUID> teamId,
      Optional<UUID> userId,
      Optional<String> segmentKey) {
    Condition condition =
        FEATURE_FLAG_OVERRIDES
            .FLAG_KEY
            .eq(flagKey)
            .and(FEATURE_FLAG_OVERRIDES.SCOPE.eq(scope.toDbValue()))
            .and(FEATURE_FLAG_OVERRIDES.DELETED_AT.isNull());

    condition =
        switch (scope) {
          case USER ->
              condition
                  .and(FEATURE_FLAG_OVERRIDES.TEAM_ID.eq(teamId.orElse(null)))
                  .and(FEATURE_FLAG_OVERRIDES.USER_ID.eq(userId.orElse(null)));
          case TEAM -> condition.and(FEATURE_FLAG_OVERRIDES.TEAM_ID.eq(teamId.orElse(null)));
          case SEGMENT ->
              condition.and(FEATURE_FLAG_OVERRIDES.SEGMENT_KEY.eq(segmentKey.orElse(null)));
        };

    return dsl.selectFrom(FEATURE_FLAG_OVERRIDES)
        .where(condition)
        .fetchOptional()
        .map(this::toDomain);
  }

  /** Fetch only TEAM-scope overrides for a given team (backoffice team view). */
  public List<FeatureFlagOverride> findTeamScopeOverrides(UUID teamId) {
    return List.copyOf(
        dsl.selectFrom(FEATURE_FLAG_OVERRIDES)
            .where(
                FEATURE_FLAG_OVERRIDES
                    .SCOPE
                    .eq(OverrideScope.TEAM.toDbValue())
                    .and(FEATURE_FLAG_OVERRIDES.TEAM_ID.eq(teamId))
                    .and(FEATURE_FLAG_OVERRIDES.DELETED_AT.isNull()))
            .orderBy(FEATURE_FLAG_OVERRIDES.FLAG_KEY.asc())
            .fetch()
            .map(this::toDomain));
  }

  /** All segment overrides (for backoffice segment view). */
  public List<FeatureFlagOverride> findAllSegmentOverrides() {
    return List.copyOf(
        dsl.selectFrom(FEATURE_FLAG_OVERRIDES)
            .where(
                FEATURE_FLAG_OVERRIDES
                    .SCOPE
                    .eq(OverrideScope.SEGMENT.toDbValue())
                    .and(FEATURE_FLAG_OVERRIDES.DELETED_AT.isNull()))
            .orderBy(
                FEATURE_FLAG_OVERRIDES.PRIORITY.asc(), FEATURE_FLAG_OVERRIDES.SEGMENT_KEY.asc())
            .fetch()
            .map(this::toDomain));
  }

  public FeatureFlagOverride save(FeatureFlagOverride override, UUID actorId) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (override.getId() == null) {
      UUID newId = UUID.randomUUID();
      dsl.insertInto(FEATURE_FLAG_OVERRIDES)
          .set(FEATURE_FLAG_OVERRIDES.ID, newId)
          .set(FEATURE_FLAG_OVERRIDES.FLAG_KEY, override.getFlagKey())
          .set(FEATURE_FLAG_OVERRIDES.SCOPE, override.getScope().toDbValue())
          .set(FEATURE_FLAG_OVERRIDES.SEGMENT_KEY, override.getSegmentKey().orElse(null))
          .set(FEATURE_FLAG_OVERRIDES.TEAM_ID, override.getTeamId().orElse(null))
          .set(FEATURE_FLAG_OVERRIDES.USER_ID, override.getUserId().orElse(null))
          .set(FEATURE_FLAG_OVERRIDES.ENABLED, override.isEnabled())
          .set(FEATURE_FLAG_OVERRIDES.VALUE, override.getValue().orElse(null))
          .set(FEATURE_FLAG_OVERRIDES.PRIORITY, override.getPriority())
          .set(FEATURE_FLAG_OVERRIDES.CREATED_AT, now)
          .set(FEATURE_FLAG_OVERRIDES.UPDATED_AT, now)
          .set(FEATURE_FLAG_OVERRIDES.CREATED_BY, actorId)
          .set(FEATURE_FLAG_OVERRIDES.UPDATED_BY, actorId)
          .execute();
      override.setId(newId);
      override.setCreatedAt(now.toInstant(UTC));
      override.setUpdatedAt(now.toInstant(UTC));
    } else {
      dsl.update(FEATURE_FLAG_OVERRIDES)
          .set(FEATURE_FLAG_OVERRIDES.ENABLED, override.isEnabled())
          .set(FEATURE_FLAG_OVERRIDES.VALUE, override.getValue().orElse(null))
          .set(FEATURE_FLAG_OVERRIDES.PRIORITY, override.getPriority())
          .set(FEATURE_FLAG_OVERRIDES.UPDATED_AT, now)
          .set(FEATURE_FLAG_OVERRIDES.UPDATED_BY, actorId)
          .where(FEATURE_FLAG_OVERRIDES.ID.eq(override.getId()))
          .execute();
      override.setUpdatedAt(now.toInstant(UTC));
    }
    return override;
  }

  public void softDelete(UUID id, UUID actorId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(FEATURE_FLAG_OVERRIDES)
        .set(FEATURE_FLAG_OVERRIDES.DELETED_AT, now)
        .set(FEATURE_FLAG_OVERRIDES.UPDATED_BY, actorId)
        .set(FEATURE_FLAG_OVERRIDES.UPDATED_AT, now)
        .where(FEATURE_FLAG_OVERRIDES.ID.eq(id).and(FEATURE_FLAG_OVERRIDES.DELETED_AT.isNull()))
        .execute();
  }

  /** Soft-delete all overrides targeting a given segment key. */
  public int softDeleteBySegmentKey(String segmentKey, UUID actorId) {
    LocalDateTime now = LocalDateTime.now(clock);
    return dsl.update(FEATURE_FLAG_OVERRIDES)
        .set(FEATURE_FLAG_OVERRIDES.DELETED_AT, now)
        .set(FEATURE_FLAG_OVERRIDES.UPDATED_BY, actorId)
        .set(FEATURE_FLAG_OVERRIDES.UPDATED_AT, now)
        .where(
            FEATURE_FLAG_OVERRIDES
                .SEGMENT_KEY
                .eq(segmentKey)
                .and(FEATURE_FLAG_OVERRIDES.DELETED_AT.isNull()))
        .execute();
  }

  private FeatureFlagOverride toDomain(FeatureFlagOverridesRecord r) {
    return FeatureFlagOverride.builder()
        .id(r.getId())
        .flagKey(r.getFlagKey())
        .scope(OverrideScope.fromDbValue(r.getScope()))
        .segmentKey(Optional.ofNullable(r.getSegmentKey()))
        .teamId(Optional.ofNullable(r.getTeamId()))
        .userId(Optional.ofNullable(r.getUserId()))
        .enabled(r.getEnabled())
        .value(Optional.ofNullable(r.getValue()))
        .priority(r.getPriority())
        .createdAt(r.getCreatedAt().toInstant(UTC))
        .updatedAt(r.getUpdatedAt().toInstant(UTC))
        .createdBy(Optional.ofNullable(r.getCreatedBy()))
        .updatedBy(Optional.ofNullable(r.getUpdatedBy()))
        .deletedAt(Optional.ofNullable(r.getDeletedAt()).map(dt -> dt.toInstant(UTC)))
        .build();
  }
}
