package com.buurman.repository.backoffice;

import static com.buurman.jooq.generated.Tables.COST_SNAPSHOT;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/** Persistence + read models for cost snapshots. The table is small (≈ providers × months). */
@Repository
@RequiredArgsConstructor
public class CostSnapshotRepository {

  private final DSLContext dsl;

  public record LatestProviderCost(
      String provider,
      String sourceType,
      String currency,
      long amountMinor,
      long amountEurMinor,
      LocalDateTime capturedAt) {}

  public record MonthlyTotal(LocalDate month, long totalEurMinor) {}

  /** Upsert this month's row for a provider in place (idempotent within a month). */
  public void upsert(
      String provider,
      String sourceType,
      LocalDate periodMonth,
      String currency,
      long amountMinor,
      long amountEurMinor,
      BigDecimal fxRate,
      String breakdownJson) {
    JSONB breakdown = breakdownJson == null ? null : JSONB.valueOf(breakdownJson);
    dsl.insertInto(COST_SNAPSHOT)
        .set(COST_SNAPSHOT.PROVIDER, provider)
        .set(COST_SNAPSHOT.SOURCE_TYPE, sourceType)
        .set(COST_SNAPSHOT.PERIOD_MONTH, periodMonth)
        .set(COST_SNAPSHOT.CURRENCY, currency)
        .set(COST_SNAPSHOT.AMOUNT_MINOR, amountMinor)
        .set(COST_SNAPSHOT.AMOUNT_EUR_MINOR, amountEurMinor)
        .set(COST_SNAPSHOT.FX_RATE, fxRate)
        .set(COST_SNAPSHOT.BREAKDOWN, breakdown)
        .onConflict(COST_SNAPSHOT.PROVIDER, COST_SNAPSHOT.PERIOD_MONTH)
        .doUpdate()
        .set(COST_SNAPSHOT.SOURCE_TYPE, sourceType)
        .set(COST_SNAPSHOT.CURRENCY, currency)
        .set(COST_SNAPSHOT.AMOUNT_MINOR, amountMinor)
        .set(COST_SNAPSHOT.AMOUNT_EUR_MINOR, amountEurMinor)
        .set(COST_SNAPSHOT.FX_RATE, fxRate)
        .set(COST_SNAPSHOT.BREAKDOWN, breakdown)
        .set(COST_SNAPSHOT.CAPTURED_AT, LocalDateTime.now())
        .execute();
  }

  /**
   * The snapshot for each provider in {@code month} (at most one row per provider, thanks to the
   * (provider, period_month) unique key). Providers with no snapshot this month are simply absent —
   * a stale prior month is never carried forward into the headline.
   */
  public List<LatestProviderCost> forMonth(LocalDate month) {
    return dsl.selectFrom(COST_SNAPSHOT)
        .where(COST_SNAPSHOT.PERIOD_MONTH.eq(month))
        .fetch()
        .map(
            r ->
                new LatestProviderCost(
                    r.getProvider(),
                    r.getSourceType(),
                    r.getCurrency(),
                    r.getAmountMinor(),
                    r.getAmountEurMinor(),
                    r.getCapturedAt()));
  }

  /** Total EUR per month (one row per provider per month), oldest first. Summed in SQL. */
  public List<MonthlyTotal> monthlyTotals(LocalDate since) {
    return dsl.select(COST_SNAPSHOT.PERIOD_MONTH, DSL.sum(COST_SNAPSHOT.AMOUNT_EUR_MINOR))
        .from(COST_SNAPSHOT)
        .where(COST_SNAPSHOT.PERIOD_MONTH.ge(since))
        .groupBy(COST_SNAPSHOT.PERIOD_MONTH)
        .orderBy(COST_SNAPSHOT.PERIOD_MONTH.asc())
        .fetch()
        .map(r -> new MonthlyTotal(r.value1(), r.value2() == null ? 0L : r.value2().longValue()));
  }
}
