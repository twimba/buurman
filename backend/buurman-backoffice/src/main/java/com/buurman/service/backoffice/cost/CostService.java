package com.buurman.service.backoffice.cost;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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

  private final List<CostSource> sources;
  private final FxConverter fxConverter;
  private final CostSnapshotRepository snapshotRepository;
  private final DashboardAggregateRepository aggregateRepository;
  private final ObjectMapper objectMapper;

  /**
   * Polls every source and persists a snapshot. Called by the daily Quartz job (no security
   * context) and by the refresh endpoint (URL-secured under {@code /backoffice/**}) — so it is not
   * {@code @PreAuthorize}-guarded itself.
   */
  @Transactional
  public void snapshotNow() {
    LocalDate periodMonth = LocalDate.now().withDayOfMonth(1);
    for (CostSource source : sources) {
      ProviderReading reading = source.read();
      if (!reading.available()) {
        continue;
      }
      Optional<Converted> eur = fxConverter.toEur(reading.currency(), reading.amountMinor());
      if (eur.isEmpty()) {
        log.warn(
            "No FX rate for {} ({}); skipping snapshot", reading.provider(), reading.currency());
        continue;
      }
      snapshotRepository.insert(
          reading.provider().name(),
          reading.type().name(),
          periodMonth,
          reading.currency(),
          reading.amountMinor(),
          eur.get().eurMinor(),
          eur.get().rate(),
          serializeBreakdown(reading));
    }
  }

  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('BACKOFFICE_ADMIN')")
  public CostOverviewResponse overview() {
    List<LatestProviderCost> latest = snapshotRepository.latestPerProvider();
    List<ProviderCost> providers = providerCosts(latest);
    long total =
        providers.stream()
            .filter(ProviderCost::available)
            .mapToLong(ProviderCost::amountEurMinor)
            .sum();

    List<MonthlyTotal> monthly =
        snapshotRepository.monthlyTotals(
            LocalDate.now().withDayOfMonth(1).minusMonths(TREND_MONTHS - 1L));
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
    LocalDate thisMonth = LocalDate.now().withDayOfMonth(1);
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
