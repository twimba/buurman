package com.buurman.repository.backoffice;

import static com.buurman.jooq.generated.Tables.COST_CONFIG;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Admin-editable provider cost parameters (generic key/value), overriding application.yml seeds.
 */
@Repository
@RequiredArgsConstructor
public class CostConfigRepository {

  private final DSLContext dsl;
  private final Clock clock;

  /** config key -> stored value. */
  public Map<String, String> all() {
    Map<String, String> result = new LinkedHashMap<>();
    dsl.select(COST_CONFIG.CONFIG_KEY, COST_CONFIG.VALUE_TEXT)
        .from(COST_CONFIG)
        .fetch()
        .forEach(r -> result.put(r.value1(), r.value2()));
    return result;
  }

  public void upsert(String key, String value, String updatedBy) {
    dsl.insertInto(COST_CONFIG)
        .set(COST_CONFIG.CONFIG_KEY, key)
        .set(COST_CONFIG.VALUE_TEXT, value)
        .set(COST_CONFIG.UPDATED_AT, LocalDateTime.now(clock))
        .set(COST_CONFIG.UPDATED_BY, updatedBy)
        .onConflict(COST_CONFIG.CONFIG_KEY)
        .doUpdate()
        .set(COST_CONFIG.VALUE_TEXT, value)
        .set(COST_CONFIG.UPDATED_AT, LocalDateTime.now(clock))
        .set(COST_CONFIG.UPDATED_BY, updatedBy)
        .execute();
  }
}
