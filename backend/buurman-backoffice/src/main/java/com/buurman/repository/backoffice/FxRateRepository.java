package com.buurman.repository.backoffice;

import static com.buurman.jooq.generated.Tables.FX_RATE;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/** Dated FX-rate history (source currency -> EUR), one row per (currency, rate_date). */
@Repository
@RequiredArgsConstructor
public class FxRateRepository {

  private final DSLContext dsl;
  private final Clock clock;

  /** A stored rate for a currency on a given day. */
  public record FxRateRow(String currency, LocalDate rateDate, BigDecimal rate, String source) {}

  /**
   * Store/replace the rate at which one unit of {@code currency} converts to EUR on {@code date}.
   */
  public void upsert(String currency, LocalDate date, BigDecimal rate, String source) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.insertInto(FX_RATE)
        .set(FX_RATE.CURRENCY, currency)
        .set(FX_RATE.RATE_DATE, date)
        .set(FX_RATE.RATE, rate)
        .set(FX_RATE.SOURCE, source)
        .set(FX_RATE.UPDATED_AT, now)
        .onConflict(FX_RATE.CURRENCY, FX_RATE.RATE_DATE)
        .doUpdate()
        .set(FX_RATE.RATE, rate)
        .set(FX_RATE.SOURCE, source)
        .set(FX_RATE.UPDATED_AT, now)
        .execute();
  }

  /** Remove the stored rate for a currency on a specific day. */
  public void delete(String currency, LocalDate date) {
    dsl.deleteFrom(FX_RATE)
        .where(FX_RATE.CURRENCY.eq(currency).and(FX_RATE.RATE_DATE.eq(date)))
        .execute();
  }

  /** Remove all stored rates for a currency (used when a pair stops being tracked). */
  public void deleteAll(String currency) {
    dsl.deleteFrom(FX_RATE).where(FX_RATE.CURRENCY.eq(currency)).execute();
  }

  /** The rate effective on {@code date}: the newest stored rate dated on or before it. */
  public Optional<BigDecimal> rateOn(String currency, LocalDate date) {
    return dsl.select(FX_RATE.RATE)
        .from(FX_RATE)
        .where(FX_RATE.CURRENCY.eq(currency).and(FX_RATE.RATE_DATE.le(date)))
        .orderBy(FX_RATE.RATE_DATE.desc())
        .limit(1)
        .fetchOptional(FX_RATE.RATE);
  }

  /** Latest stored rate per currency (for the admin FX view). */
  public List<FxRateRow> latestPerCurrency() {
    Set<String> seen = new HashSet<>();
    List<FxRateRow> out = new ArrayList<>();
    dsl.select(FX_RATE.CURRENCY, FX_RATE.RATE_DATE, FX_RATE.RATE, FX_RATE.SOURCE)
        .from(FX_RATE)
        .orderBy(FX_RATE.CURRENCY.asc(), FX_RATE.RATE_DATE.desc())
        .fetch()
        .forEach(
            r -> {
              if (seen.add(r.value1())) {
                out.add(new FxRateRow(r.value1(), r.value2(), r.value3(), r.value4()));
              }
            });
    return out;
  }

  /** Most recent {@code limit} rows across all currencies, newest first (history table). */
  public List<FxRateRow> allRecent(int limit) {
    return dsl.select(FX_RATE.CURRENCY, FX_RATE.RATE_DATE, FX_RATE.RATE, FX_RATE.SOURCE)
        .from(FX_RATE)
        .orderBy(FX_RATE.RATE_DATE.desc(), FX_RATE.CURRENCY.asc())
        .limit(limit)
        .fetch(r -> new FxRateRow(r.value1(), r.value2(), r.value3(), r.value4()));
  }

  /** Most recent {@code limit} daily rates for one currency, newest first (history view). */
  public List<FxRateRow> history(String currency, int limit) {
    return dsl.select(FX_RATE.CURRENCY, FX_RATE.RATE_DATE, FX_RATE.RATE, FX_RATE.SOURCE)
        .from(FX_RATE)
        .where(FX_RATE.CURRENCY.eq(currency))
        .orderBy(FX_RATE.RATE_DATE.desc())
        .limit(limit)
        .fetch(r -> new FxRateRow(r.value1(), r.value2(), r.value3(), r.value4()));
  }
}
