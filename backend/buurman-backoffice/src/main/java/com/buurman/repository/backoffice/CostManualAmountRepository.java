package com.buurman.repository.backoffice;

import static com.buurman.jooq.generated.Tables.COST_MANUAL_AMOUNT;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/** Admin-editable monthly EUR cost amounts per provider. */
@Repository
@RequiredArgsConstructor
public class CostManualAmountRepository {

  private final DSLContext dsl;
  private final Clock clock;

  /** provider -> EUR minor units. */
  public Map<String, Long> all() {
    Map<String, Long> result = new LinkedHashMap<>();
    dsl.select(COST_MANUAL_AMOUNT.PROVIDER, COST_MANUAL_AMOUNT.AMOUNT_EUR_MINOR)
        .from(COST_MANUAL_AMOUNT)
        .fetch()
        .forEach(r -> result.put(r.value1(), r.value2()));
    return result;
  }

  public void upsert(String provider, long amountEurMinor, String updatedBy) {
    dsl.insertInto(COST_MANUAL_AMOUNT)
        .set(COST_MANUAL_AMOUNT.PROVIDER, provider)
        .set(COST_MANUAL_AMOUNT.AMOUNT_EUR_MINOR, amountEurMinor)
        .set(COST_MANUAL_AMOUNT.UPDATED_AT, LocalDateTime.now(clock))
        .set(COST_MANUAL_AMOUNT.UPDATED_BY, updatedBy)
        .onConflict(COST_MANUAL_AMOUNT.PROVIDER)
        .doUpdate()
        .set(COST_MANUAL_AMOUNT.AMOUNT_EUR_MINOR, amountEurMinor)
        .set(COST_MANUAL_AMOUNT.UPDATED_AT, LocalDateTime.now(clock))
        .set(COST_MANUAL_AMOUNT.UPDATED_BY, updatedBy)
        .execute();
  }
}
