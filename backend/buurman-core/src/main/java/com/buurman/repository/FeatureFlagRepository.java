package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.FEATURE_FLAGS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.FeatureFlag;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class FeatureFlagRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public List<FeatureFlag> findAll() {
    return List.copyOf(
        dsl.selectFrom(FEATURE_FLAGS)
            .where(FEATURE_FLAGS.DELETED_AT.isNull())
            .orderBy(FEATURE_FLAGS.KEY.asc())
            .fetch()
            .map(this::toDomain));
  }

  public Optional<FeatureFlag> findByKey(String key) {
    return dsl.selectFrom(FEATURE_FLAGS)
        .where(FEATURE_FLAGS.KEY.eq(key).and(FEATURE_FLAGS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(this::toDomain);
  }

  public FeatureFlag save(FeatureFlag flag, UUID actorId) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (flag.getId() == null) {
      UUID newId = UUID.randomUUID();
      dsl.insertInto(FEATURE_FLAGS)
          .set(FEATURE_FLAGS.ID, newId)
          .set(FEATURE_FLAGS.KEY, flag.getKey())
          .set(FEATURE_FLAGS.VALUE_TYPE, flag.getValueType())
          .set(FEATURE_FLAGS.DEFAULT_ENABLED, flag.isDefaultEnabled())
          .set(FEATURE_FLAGS.DEFAULT_VALUE, flag.getDefaultValue().orElse(null))
          .set(FEATURE_FLAGS.DESCRIPTION, flag.getDescription().orElse(null))
          .set(FEATURE_FLAGS.CREATED_AT, now)
          .set(FEATURE_FLAGS.UPDATED_AT, now)
          .set(FEATURE_FLAGS.CREATED_BY, actorId)
          .set(FEATURE_FLAGS.UPDATED_BY, actorId)
          .execute();
      flag.setId(newId);
      flag.setCreatedAt(now.toInstant(UTC));
      flag.setUpdatedAt(now.toInstant(UTC));
    } else {
      dsl.update(FEATURE_FLAGS)
          .set(FEATURE_FLAGS.DEFAULT_ENABLED, flag.isDefaultEnabled())
          .set(FEATURE_FLAGS.DEFAULT_VALUE, flag.getDefaultValue().orElse(null))
          .set(FEATURE_FLAGS.DESCRIPTION, flag.getDescription().orElse(null))
          .set(FEATURE_FLAGS.UPDATED_AT, now)
          .set(FEATURE_FLAGS.UPDATED_BY, actorId)
          .where(FEATURE_FLAGS.ID.eq(flag.getId()))
          .execute();
      flag.setUpdatedAt(now.toInstant(UTC));
    }
    return flag;
  }

  public void softDelete(String key, UUID actorId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(FEATURE_FLAGS)
        .set(FEATURE_FLAGS.DELETED_AT, now)
        .set(FEATURE_FLAGS.UPDATED_BY, actorId)
        .set(FEATURE_FLAGS.UPDATED_AT, now)
        .where(FEATURE_FLAGS.KEY.eq(key).and(FEATURE_FLAGS.DELETED_AT.isNull()))
        .execute();
  }

  private FeatureFlag toDomain(com.buurman.jooq.generated.tables.records.FeatureFlagsRecord r) {
    return FeatureFlag.builder()
        .id(r.getId())
        .key(r.getKey())
        .valueType(r.getValueType())
        .defaultEnabled(r.getDefaultEnabled())
        .defaultValue(Optional.ofNullable(r.getDefaultValue()))
        .description(Optional.ofNullable(r.getDescription()))
        .createdAt(r.getCreatedAt().toInstant(UTC))
        .updatedAt(r.getUpdatedAt().toInstant(UTC))
        .createdBy(Optional.ofNullable(r.getCreatedBy()))
        .updatedBy(Optional.ofNullable(r.getUpdatedBy()))
        .deletedAt(Optional.ofNullable(r.getDeletedAt()).map(dt -> dt.toInstant(UTC)))
        .build();
  }
}
