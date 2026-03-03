package com.buurman.repository;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import com.buurman.domain.RateLimitConfig;

import lombok.RequiredArgsConstructor;
import com.buurman.domain.Ulid;

@Repository
@RequiredArgsConstructor
public class RateLimitConfigRepository {

  private final DSLContext dsl;
  private final Clock clock;

  private static final org.jooq.Table<?> TABLE = table("rate_limit_config");
  private static final org.jooq.Field<UUID> ID = field("id", UUID.class);
  private static final org.jooq.Field<String> KEY = field("key", String.class);
  private static final org.jooq.Field<String> DISPLAY_NAME = field("display_name", String.class);
  private static final org.jooq.Field<String> DESCRIPTION = field("description", String.class);
  private static final org.jooq.Field<Integer> MAX_REQUESTS = field("max_requests", Integer.class);
  private static final org.jooq.Field<Integer> PERIOD_SECONDS =
      field("period_seconds", Integer.class);
  private static final org.jooq.Field<Boolean> ENABLED = field("enabled", Boolean.class);
  private static final org.jooq.Field<Timestamp> UPDATED_AT = field("updated_at", Timestamp.class);
  private static final org.jooq.Field<String> UPDATED_BY = field("updated_by", String.class);

  public List<RateLimitConfig> findAll() {
    return dsl.select().from(TABLE).orderBy(KEY).fetch(this::toDomain);
  }

  public Optional<RateLimitConfig> findByKey(String key) {
    return dsl.select().from(TABLE).where(KEY.eq(key)).fetchOptional().map(this::toDomain);
  }

  public RateLimitConfig save(RateLimitConfig config) {
    Timestamp now = Timestamp.from(clock.instant());

    dsl.update(TABLE)
        .set(MAX_REQUESTS, config.getMaxRequests())
        .set(PERIOD_SECONDS, config.getPeriodSeconds())
        .set(ENABLED, config.isEnabled())
        .set(UPDATED_AT, now)
        .set(UPDATED_BY, config.getUpdatedBy().orElse(null))
        .where(KEY.eq(config.getKey()))
        .execute();

    config.setUpdatedAt(Optional.of(now.toInstant()));
    return config;
  }

  private RateLimitConfig toDomain(Record record) {
    RateLimitConfig config = new RateLimitConfig();
    config.setId(record.get(ID));
    config.setKey(record.get(KEY));
    config.setDisplayName(record.get(DISPLAY_NAME));
    config.setDescription(Optional.ofNullable(record.get(DESCRIPTION)));
    Integer maxRequests = record.get(MAX_REQUESTS);
    if (maxRequests != null) {
      config.setMaxRequests(maxRequests);
    }
    Integer periodSeconds = record.get(PERIOD_SECONDS);
    if (periodSeconds != null) {
      config.setPeriodSeconds(periodSeconds);
    }
    Boolean enabled = record.get(ENABLED);
    if (enabled != null) {
      config.setEnabled(enabled);
    }
    Timestamp updatedAtTs = record.get(UPDATED_AT);
    if (updatedAtTs != null) {
      config.setUpdatedAt(Optional.of(updatedAtTs.toInstant()));
    }
    config.setUpdatedBy(Optional.ofNullable(record.get(UPDATED_BY)));
    return config;
  }
}
