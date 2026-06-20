package com.buurman.repository.backoffice;

import static com.buurman.jooq.generated.Tables.COST_SNAPSHOT;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/** Persistence + read models for cost snapshots. The table is small (≈ providers × days). */
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

  public void insert(
      String provider,
      String sourceType,
      LocalDate periodMonth,
      String currency,
      long amountMinor,
      long amountEurMinor,
      BigDecimal fxRate,
      String breakdownJson) {
    dsl.insertInto(COST_SNAPSHOT)
        .set(COST_SNAPSHOT.PROVIDER, provider)
        .set(COST_SNAPSHOT.SOURCE_TYPE, sourceType)
        .set(COST_SNAPSHOT.PERIOD_MONTH, periodMonth)
        .set(COST_SNAPSHOT.CURRENCY, currency)
        .set(COST_SNAPSHOT.AMOUNT_MINOR, amountMinor)
        .set(COST_SNAPSHOT.AMOUNT_EUR_MINOR, amountEurMinor)
        .set(COST_SNAPSHOT.FX_RATE, fxRate)
        .set(COST_SNAPSHOT.BREAKDOWN, breakdownJson == null ? null : JSONB.valueOf(breakdownJson))
        .execute();
  }

  /** The most recent snapshot for each provider. */
  public List<LatestProviderCost> latestPerProvider() {
    Map<String, LatestProviderCost> latest = new LinkedHashMap<>();
    dsl.selectFrom(COST_SNAPSHOT)
        .orderBy(COST_SNAPSHOT.CAPTURED_AT.desc())
        .fetch()
        .forEach(
            r ->
                latest.computeIfAbsent(
                    r.getProvider(),
                    k ->
                        new LatestProviderCost(
                            r.getProvider(),
                            r.getSourceType(),
                            r.getCurrency(),
                            r.getAmountMinor(),
                            r.getAmountEurMinor(),
                            r.getCapturedAt())));
    return new ArrayList<>(latest.values());
  }

  /** Total EUR per month (latest snapshot per provider within each month), oldest first. */
  public List<MonthlyTotal> monthlyTotals(LocalDate since) {
    // (provider, month) -> latest eur (rows arrive newest-first, so keep the first seen).
    Map<String, Long> latestPerProviderMonth = new LinkedHashMap<>();
    dsl.selectFrom(COST_SNAPSHOT)
        .where(COST_SNAPSHOT.PERIOD_MONTH.ge(since))
        .orderBy(COST_SNAPSHOT.CAPTURED_AT.desc())
        .fetch()
        .forEach(
            r ->
                latestPerProviderMonth.computeIfAbsent(
                    r.getPeriodMonth() + "|" + r.getProvider(), k -> r.getAmountEurMinor()));

    TreeMap<LocalDate, Long> byMonth = new TreeMap<>();
    latestPerProviderMonth.forEach(
        (key, eur) -> {
          LocalDate month = LocalDate.parse(key.substring(0, key.indexOf('|')));
          byMonth.merge(month, eur, Long::sum);
        });

    List<MonthlyTotal> totals = new ArrayList<>();
    byMonth.forEach((month, eur) -> totals.add(new MonthlyTotal(month, eur)));
    return totals;
  }
}
