package com.buurman.repository.backoffice;

import static com.buurman.jooq.generated.Tables.FX_PAIR;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Tracked FX currency pairs (all anchored on EUR). Drives which currencies refresh/backfill fetch.
 */
@Repository
@RequiredArgsConstructor
public class FxPairRepository {

  private final DSLContext dsl;
  private final Clock clock;

  /** Tracked source currencies, alphabetical. */
  public List<String> list() {
    return dsl.select(FX_PAIR.CURRENCY)
        .from(FX_PAIR)
        .orderBy(FX_PAIR.CURRENCY.asc())
        .fetch(FX_PAIR.CURRENCY);
  }

  public boolean exists(String currency) {
    return dsl.fetchExists(dsl.selectFrom(FX_PAIR).where(FX_PAIR.CURRENCY.eq(currency)));
  }

  public void add(String currency, String by) {
    dsl.insertInto(FX_PAIR)
        .set(FX_PAIR.CURRENCY, currency)
        .set(FX_PAIR.CREATED_AT, LocalDateTime.now(clock))
        .set(FX_PAIR.CREATED_BY, by)
        .onConflict(FX_PAIR.CURRENCY)
        .doNothing()
        .execute();
  }

  public void remove(String currency) {
    dsl.deleteFrom(FX_PAIR).where(FX_PAIR.CURRENCY.eq(currency)).execute();
  }
}
