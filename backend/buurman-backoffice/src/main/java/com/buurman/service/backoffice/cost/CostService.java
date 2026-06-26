package com.buurman.service.backoffice.cost;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.backoffice.CostProviderId;
import com.buurman.domain.backoffice.CostSourceType;
import com.buurman.domain.backoffice.PanelStatus;
import com.buurman.dto.response.backoffice.cost.CostInsight;
import com.buurman.dto.response.backoffice.cost.CostOverviewResponse;
import com.buurman.dto.response.backoffice.cost.CostTrendPoint;
import com.buurman.dto.response.backoffice.cost.CostWatchResponse;
import com.buurman.dto.response.backoffice.cost.ProviderCost;
import com.buurman.repository.backoffice.CostSnapshotRepository;
import com.buurman.repository.backoffice.CostSnapshotRepository.LatestProviderCost;
import com.buurman.repository.backoffice.CostSnapshotRepository.MonthlyTotal;
import com.buurman.repository.backoffice.DashboardAggregateRepository;
import com.buurman.service.backoffice.cost.CostSource.ProviderReading;
import com.buurman.service.backoffice.cost.FxConverter.Converted;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Aggregates per-provider cost snapshots into the panel summary, the Costs page, and insights. */
@Service
@Slf4j
@RequiredArgsConstructor
public class CostService {

  private static final int TREND_MONTHS = 6;
  private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

  private final Clock clock;
  private final List<CostSource> apiSources;
  private final ManualCostResolver manualResolver;
  private final com.buurman.repository.backoffice.CostManualAmountRepository manualAmountRepository;
  private final FxConverter fxConverter;
  private final CostSnapshotRepository snapshotRepository;
  private final DashboardAggregateRepository aggregateRepository;
  private final ObjectMapper objectMapper;

  /**
   * Self-reference through the Spring proxy so {@link #persistSnapshots(List)} runs inside its own
   * transaction when invoked from the non-transactional {@link #snapshotNow()}. A plain {@code
   * this.persistSnapshots(...)} would be self-invocation and bypass the proxy (no tx).
   */
  private final ObjectProvider<CostService> self;

  /** A fully-resolved snapshot row, ready to persist (provider HTTP + FX already done). */
  private record ResolvedSnapshot(
      String provider,
      String sourceType,
      LocalDate periodMonth,
      String currency,
      long amountMinor,
      long amountEurMinor,
      java.math.BigDecimal rate,
      String breakdown) {}

  /**
   * Admin-triggered refresh. Carries an explicit method-security check as defense-in-depth so the
   * outbound provider fan-out can't be un-gated by a future change to the URL matcher alone.
   */
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public void refreshNow() {
    snapshotNow();
  }

  /**
   * Polls every source and persists a snapshot. Called by the daily Quartz job (no security
   * context) and by {@link #refreshNow()} for the refresh endpoint — so it is not
   * {@code @PreAuthorize}-guarded itself.
   *
   * <p>Deliberately NOT {@code @Transactional}: the per-provider readings (blocking outbound HTTP)
   * and FX conversion happen here, outside any transaction, so a DB connection is never held open
   * across the slow provider fan-out. Only the short {@link #persistSnapshots(List)} that follows
   * runs in a transaction (invoked through the proxy via {@link #self}).
   */
  public void snapshotNow() {
    List<ResolvedSnapshot> rows = resolveSnapshots();
    if (!rows.isEmpty()) {
      self.getObject().persistSnapshots(rows);
    }
  }

  /** Resolve every provider's reading + EUR conversion. No DB transaction; does the slow I/O. */
  private List<ResolvedSnapshot> resolveSnapshots() {
    LocalDate periodMonth = LocalDate.now(clock).withDayOfMonth(1);
    java.util.Map<CostProviderId, CostSource> byId = new java.util.EnumMap<>(CostProviderId.class);
    apiSources.forEach(s -> byId.put(s.id(), s));

    List<ResolvedSnapshot> rows = new ArrayList<>();
    for (CostProviderId id : CostProviderId.values()) {
      // Isolate each provider: a transient failure (e.g. the manual-amount DB lookup) must not
      // abort the whole snapshot and lose the providers that resolved cleanly.
      try {
        ProviderReading reading = resolveReading(id, byId.get(id));
        if (reading == null) {
          continue;
        }
        // EUR is frozen at capture-time FX: amount_eur_minor records the rate effective today and
        // is NOT re-normalized if a past rate is later backfilled/corrected. Backfilling FX
        // therefore only affects future captures, not historical snapshots — the stored figure is
        // the historical record. (See FxConverter.)
        Optional<Converted> eur =
            fxConverter.toEur(reading.currency(), reading.amountMinor(), LocalDate.now(clock));
        if (eur.isEmpty()) {
          log.warn("No FX rate for {} ({}); skipping snapshot", id, reading.currency());
          continue;
        }
        rows.add(
            new ResolvedSnapshot(
                id.name(),
                reading.type().name(),
                periodMonth,
                reading.currency(),
                reading.amountMinor(),
                eur.get().eurMinor(),
                eur.get().rate(),
                serializeBreakdown(reading)));
      } catch (RuntimeException e) {
        log.warn("Failed to resolve cost snapshot for {}; skipping", id, e);
      }
    }
    return rows;
  }

  /** Short write-only transaction: persist the already-resolved snapshot rows. */
  @Transactional
  public void persistSnapshots(List<ResolvedSnapshot> rows) {
    for (ResolvedSnapshot r : rows) {
      snapshotRepository.upsert(
          r.provider(),
          r.sourceType(),
          r.periodMonth(),
          r.currency(),
          r.amountMinor(),
          r.amountEurMinor(),
          r.rate(),
          r.breakdown());
    }
  }

  /** API reading when available, else the admin-edited/configured manual amount, else null. */
  private ProviderReading resolveReading(CostProviderId id, CostSource apiSource) {
    if (apiSource != null) {
      ProviderReading reading = apiSource.read();
      if (reading.available()) {
        return reading;
      }
    }
    return manualResolver
        .amountEurMinor(id)
        .map(
            eurMinor ->
                ProviderReading.of(id, CostSourceType.SUBSCRIPTION, "EUR", eurMinor, List.of()))
        .orElse(null);
  }

  /** Set/override a provider's monthly EUR cost from the Costs page. */
  @Transactional
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public CostOverviewResponse setManualAmount(
      CostProviderId provider, long amountEurMinor, String updatedBy) {
    manualAmountRepository.upsert(provider.name(), Math.max(0, amountEurMinor), updatedBy);
    return overview();
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public CostOverviewResponse overview() {
    // Headline + provider list are scoped to the current month so a provider that stopped being
    // snapshotted can't carry a stale prior month into the run-rate total.
    LocalDate currentMonth = LocalDate.now(clock).withDayOfMonth(1);
    List<LatestProviderCost> latest = snapshotRepository.forMonth(currentMonth);
    List<ProviderCost> providers = providerCosts(latest);
    long total =
        providers.stream()
            .filter(ProviderCost::available)
            .mapToLong(ProviderCost::amountEurMinor)
            .sum();

    List<MonthlyTotal> monthly =
        snapshotRepository.monthlyTotals(
            LocalDate.now(clock).withDayOfMonth(1).minusMonths(TREND_MONTHS - 1L));
    List<CostTrendPoint> trend =
        monthly.stream()
            .map(m -> new CostTrendPoint(MONTH.format(m.month()), m.totalEurMinor()))
            .toList();
    Optional<Double> mom = monthOverMonth(monthly);

    List<CostInsight> insights = insights(total, mom);
    Optional<String> asOf =
        latest.stream()
            .map(LatestProviderCost::capturedAt)
            .max(java.util.Comparator.naturalOrder())
            .map(Object::toString);

    return new CostOverviewResponse(
        PanelStatus.LIVE, asOf, "EUR", total, mom, providers, trend, insights);
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public CostWatchResponse costWatch() {
    CostOverviewResponse o = overview();
    boolean anyLive = o.providers().stream().anyMatch(ProviderCost::available);
    if (!anyLive) {
      return new CostWatchResponse(
          PanelStatus.PREVIEW,
          Optional.of("Configure cost providers to enable"),
          Optional.empty(),
          0,
          Optional.empty(),
          List.of(),
          Optional.empty(),
          Optional.empty());
    }
    List<ProviderCost> top =
        o.providers().stream()
            .filter(ProviderCost::available)
            .sorted(java.util.Comparator.comparingLong(ProviderCost::amountEurMinor).reversed())
            .limit(3)
            .toList();
    Optional<CostInsight> headline = o.insights().stream().findFirst();
    return new CostWatchResponse(
        PanelStatus.LIVE,
        Optional.empty(),
        Optional.empty(),
        o.totalMonthlyEurMinor(),
        o.momChangePct(),
        top,
        headline,
        o.asOf());
  }

  /**
   * One entry per known provider, merging in the latest snapshot (or an "awaiting" placeholder).
   */
  private List<ProviderCost> providerCosts(List<LatestProviderCost> latest) {
    List<ProviderCost> result = new ArrayList<>();
    for (CostProviderId id : CostProviderId.values()) {
      Optional<LatestProviderCost> snap =
          latest.stream().filter(l -> l.provider().equals(id.name())).findFirst();
      if (snap.isPresent()) {
        LatestProviderCost s = snap.get();
        result.add(
            new ProviderCost(
                id.name(),
                id.displayName(),
                CostSourceType.valueOf(s.sourceType()),
                s.currency(),
                s.amountMinor(),
                s.amountEurMinor(),
                true,
                Optional.empty()));
      } else {
        result.add(
            new ProviderCost(
                id.name(),
                id.displayName(),
                CostSourceType.SUBSCRIPTION,
                "EUR",
                0,
                0,
                false,
                Optional.of("Awaiting first snapshot")));
      }
    }
    result.sort(java.util.Comparator.comparingLong(ProviderCost::amountEurMinor).reversed());
    return result;
  }

  private List<CostInsight> insights(long totalEurMinor, Optional<Double> mom) {
    List<CostInsight> insights = new ArrayList<>();
    long activeTeams = aggregateRepository.countActiveTeams();
    if (activeTeams > 0) {
      insights.add(
          new CostInsight(
              "Cost to serve / team",
              formatEur(Math.round((double) totalEurMinor / activeTeams)),
              Optional.of(activeTeams + " active teams")));
    }
    mom.ifPresent(
        pct ->
            insights.add(
                new CostInsight(
                    "Month over month",
                    String.format(Locale.ROOT, "%+.1f%%", pct),
                    Optional.of("vs last month"))));
    insights.add(
        new CostInsight(
            "Monthly run-rate", formatEur(totalEurMinor), Optional.of("all providers")));
    return insights;
  }

  /** % change of the current period vs the previous one, when both are present. */
  private Optional<Double> monthOverMonth(List<MonthlyTotal> monthly) {
    if (monthly.size() < 2) {
      return Optional.empty();
    }
    LocalDate thisMonth = LocalDate.now(clock).withDayOfMonth(1);
    LocalDate prevMonth = YearMonth.from(thisMonth).minusMonths(1).atDay(1);
    Long cur = find(monthly, thisMonth);
    Long prev = find(monthly, prevMonth);
    if (cur == null || prev == null || prev == 0) {
      return Optional.empty();
    }
    return Optional.of((cur - prev) * 100.0 / prev);
  }

  private static Long find(List<MonthlyTotal> monthly, LocalDate month) {
    return monthly.stream()
        .filter(m -> m.month().equals(month))
        .map(MonthlyTotal::totalEurMinor)
        .findFirst()
        .orElse(null);
  }

  private String serializeBreakdown(ProviderReading reading) {
    if (reading.breakdown() == null || reading.breakdown().isEmpty()) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(reading.breakdown());
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      return null;
    }
  }

  private static String formatEur(long minor) {
    return String.format(Locale.ROOT, "€%,.2f", minor / 100.0);
  }
}
