package com.buurman.repository.backoffice;

import java.util.List;
import java.util.Optional;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RateLimitBucketRepository {

  private final DSLContext dsl;

  private static final Table<?> TABLE = DSL.table("rate_limit_buckets");
  private static final Field<String> ID = DSL.field("id", String.class);
  private static final Field<Long> EXPIRES_AT = DSL.field("expires_at", Long.class);

  public record BucketRow(String id, long expiresAt) {}

  /** List active (non-expired) buckets with optional filters, paginated. */
  public List<BucketRow> findActive(
      Optional<String> configKeyFilter, Optional<String> clientIpFilter, int page, int size) {
    Condition condition = EXPIRES_AT.greaterThan(System.currentTimeMillis());

    if (configKeyFilter.isPresent()) {
      condition = condition.and(ID.like(configKeyFilter.get() + ":%"));
    }
    if (clientIpFilter.isPresent()) {
      condition = condition.and(ID.like("%:" + clientIpFilter.get() + "%"));
    }

    return dsl.select(ID, EXPIRES_AT)
        .from(TABLE)
        .where(condition)
        .orderBy(EXPIRES_AT.desc())
        .limit(size)
        .offset(page * size)
        .fetch(r -> new BucketRow(r.get(ID), r.get(EXPIRES_AT)));
  }

  /** Count active buckets matching filters. */
  public long countActive(Optional<String> configKeyFilter, Optional<String> clientIpFilter) {
    Condition condition = EXPIRES_AT.greaterThan(System.currentTimeMillis());

    if (configKeyFilter.isPresent()) {
      condition = condition.and(ID.like(configKeyFilter.get() + ":%"));
    }
    if (clientIpFilter.isPresent()) {
      condition = condition.and(ID.like("%:" + clientIpFilter.get() + "%"));
    }

    Long result = dsl.selectCount().from(TABLE).where(condition).fetchOne(0, Long.class);
    return result != null ? result : 0L;
  }

  /** Check if a bucket exists. */
  public boolean exists(String bucketId) {
    return dsl.fetchExists(dsl.selectOne().from(TABLE).where(ID.eq(bucketId)));
  }

  /** Delete a single bucket by ID. */
  public boolean delete(String bucketId) {
    return dsl.deleteFrom(TABLE).where(ID.eq(bucketId)).execute() > 0;
  }

  /** Delete all buckets for a config key. Returns number deleted. */
  public int deleteByConfigKey(String configKey) {
    return dsl.deleteFrom(TABLE).where(ID.like(configKey + ":%")).execute();
  }

  /** Count active buckets grouped by config key. */
  public List<Record> countByConfigKey() {
    return dsl.fetch(
        """
        SELECT split_part(id, ':', 1) AS config_key, COUNT(*) AS bucket_count
        FROM rate_limit_buckets
        WHERE expires_at > ?
        GROUP BY split_part(id, ':', 1)
        ORDER BY split_part(id, ':', 1)
        """,
        System.currentTimeMillis());
  }
}
