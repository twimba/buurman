package com.buurman.service;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Property;
import com.buurman.domain.Sid;
import com.buurman.dto.response.PortfolioDashboardResponse;
import com.buurman.dto.response.PortfolioDashboardResponse.AllocationData;
import com.buurman.dto.response.PortfolioDashboardResponse.AllocationSlice;
import com.buurman.dto.response.PortfolioDashboardResponse.EquityCompositionData;
import com.buurman.dto.response.PortfolioDashboardResponse.PortfolioSummary;
import com.buurman.dto.response.PortfolioDashboardResponse.PropertyEquity;
import com.buurman.dto.response.PortfolioDashboardResponse.PropertyPerformance;
import com.buurman.dto.response.PropertyDashboardResponse;
import com.buurman.dto.response.PropertyDashboardResponse.CashFlowChartData;
import com.buurman.dto.response.PropertyDashboardResponse.MonthlyDataPoint;
import com.buurman.dto.response.PropertyDashboardResponse.OccupancyChartData;
import com.buurman.dto.response.PropertyDashboardResponse.OccupancyDataPoint;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PortfolioDashboardService {

  private static final Logger log = LoggerFactory.getLogger(PortfolioDashboardService.class);
  private static final int MONETARY_SCALE = 2;
  private static final int PERCENT_SCALE = 1;
  private static final int DEFAULT_MONTHS = 12;
  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
  private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

  private final PropertyRepository propertyRepository;
  private final PropertyDashboardService propertyDashboardService;
  private final TeamService teamService;

  /** Holds a property together with its dashboard data for aggregation. */
  private record PropertyData(Property property, PropertyDashboardResponse dashboard) {}

  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public PortfolioDashboardResponse getPortfolioDashboard(
      Optional<Integer> months, UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();
    int effectiveMonths = months.orElse(0);
    String defaultCurrency = teamService.getDefaultCurrency(teamId);

    List<Property> allProperties = propertyRepository.findAllByTeamId(teamId);

    // Load dashboard data for each property, skipping failures
    List<PropertyData> propertyDataList = new ArrayList<>();
    for (Property property : allProperties) {
      if (property.getIdentifier().isEmpty()) {
        continue;
      }
      Sid identifier = property.getIdentifier().get();
      try {
        PropertyDashboardResponse dashboard =
            propertyDashboardService.getDashboardData(identifier, effectiveMonths, teamId);
        propertyDataList.add(new PropertyData(property, dashboard));
      } catch (Exception e) {
        log.warn("Failed to load dashboard data for property {}: {}", identifier, e.getMessage());
      }
    }

    // Determine currency match for each property
    List<PropertyData> sameCurrencyProperties =
        propertyDataList.stream()
            .filter(
                pd -> pd.dashboard().summary().currency().map(defaultCurrency::equals).orElse(true))
            .toList();

    int propertiesWithFinancialData =
        (int)
            propertyDataList.stream()
                .filter(pd -> pd.dashboard().dataCompleteness().completenessPercent() > 0)
                .count();

    PortfolioSummary summary =
        buildSummary(propertyDataList, sameCurrencyProperties, defaultCurrency);
    CashFlowChartData cashFlow = aggregateCashFlow(sameCurrencyProperties);
    OccupancyChartData occupancy = aggregateOccupancy(propertyDataList);
    List<PropertyPerformance> propertyComparison =
        buildPropertyComparison(propertyDataList, defaultCurrency);
    AllocationData allocation = buildAllocation(propertyDataList, sameCurrencyProperties);
    EquityCompositionData equityComposition =
        buildEquityComposition(propertyDataList, defaultCurrency);

    return new PortfolioDashboardResponse(
        summary,
        cashFlow,
        propertyComparison,
        allocation,
        occupancy,
        equityComposition,
        propertiesWithFinancialData,
        allProperties.size(),
        Optional.of(defaultCurrency));
  }

  private PortfolioSummary buildSummary(
      List<PropertyData> allData, List<PropertyData> sameCurrency, String defaultCurrency) {

    // Monetary sums: only same-currency properties
    BigDecimal totalPortfolioValue = ZERO;
    BigDecimal totalEquity = ZERO;
    BigDecimal totalMonthlyCashFlow = ZERO;
    BigDecimal totalAnnualNoi = ZERO;
    boolean hasAnyValue = false;
    boolean hasAnyEquity = false;
    boolean hasAnyCashFlow = false;
    boolean hasAnyNoi = false;

    for (PropertyData pd : sameCurrency) {
      PropertyDashboardResponse.EquityChartData equity = pd.dashboard().equity();
      PropertyDashboardResponse.SummaryMetrics summary = pd.dashboard().summary();

      if (equity.currentMarketValue().isPresent()) {
        totalPortfolioValue = totalPortfolioValue.add(equity.currentMarketValue().get());
        hasAnyValue = true;
      }
      if (summary.totalEquity().isPresent()) {
        totalEquity = totalEquity.add(summary.totalEquity().get());
        hasAnyEquity = true;
      }
      if (summary.monthlyCashFlow().isPresent()) {
        totalMonthlyCashFlow = totalMonthlyCashFlow.add(summary.monthlyCashFlow().get());
        hasAnyCashFlow = true;
      }
      if (summary.annualNoi().isPresent()) {
        totalAnnualNoi = totalAnnualNoi.add(summary.annualNoi().get());
        hasAnyNoi = true;
      }
    }

    // Weighted cap rate: sum(capRate * marketValue) / sum(marketValue)
    BigDecimal capRateWeightedSum = ZERO;
    BigDecimal capRateWeightBase = ZERO;
    for (PropertyData pd : sameCurrency) {
      Optional<BigDecimal> capRate = pd.dashboard().summary().capRatePercent();
      Optional<BigDecimal> marketValue = pd.dashboard().equity().currentMarketValue();
      if (capRate.isPresent() && marketValue.isPresent() && marketValue.get().compareTo(ZERO) > 0) {
        capRateWeightedSum = capRateWeightedSum.add(capRate.get().multiply(marketValue.get()));
        capRateWeightBase = capRateWeightBase.add(marketValue.get());
      }
    }
    Optional<BigDecimal> weightedCapRate =
        capRateWeightBase.compareTo(ZERO) > 0
            ? Optional.of(capRateWeightedSum.divide(capRateWeightBase, PERCENT_SCALE, HALF_UP))
            : Optional.empty();

    // Weighted cash-on-cash: sum(cashOnCash * purchasePrice) / sum(purchasePrice)
    BigDecimal cocWeightedSum = ZERO;
    BigDecimal cocWeightBase = ZERO;
    for (PropertyData pd : sameCurrency) {
      Optional<BigDecimal> coc = pd.dashboard().summary().cashOnCashPercent();
      Optional<BigDecimal> purchasePrice = pd.dashboard().equity().purchasePrice();
      if (coc.isPresent() && purchasePrice.isPresent() && purchasePrice.get().compareTo(ZERO) > 0) {
        cocWeightedSum = cocWeightedSum.add(coc.get().multiply(purchasePrice.get()));
        cocWeightBase = cocWeightBase.add(purchasePrice.get());
      }
    }
    Optional<BigDecimal> weightedCashOnCash =
        cocWeightBase.compareTo(ZERO) > 0
            ? Optional.of(cocWeightedSum.divide(cocWeightBase, PERCENT_SCALE, HALF_UP))
            : Optional.empty();

    // Portfolio occupancy: average across all properties
    List<BigDecimal> occupancyRates =
        allData.stream()
            .map(pd -> pd.dashboard().summary().occupancyRatePercent())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    Optional<BigDecimal> portfolioOccupancy =
        occupancyRates.isEmpty()
            ? Optional.empty()
            : Optional.of(
                occupancyRates.stream()
                    .reduce(ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(occupancyRates.size()), PERCENT_SCALE, HALF_UP));

    // Debt-to-equity: sum(mortgageBalance) / sum(equity)
    BigDecimal totalMortgage = ZERO;
    BigDecimal totalEquityForRatio = ZERO;
    for (PropertyData pd : sameCurrency) {
      if (pd.dashboard().equity().mortgageBalance().isPresent()) {
        totalMortgage = totalMortgage.add(pd.dashboard().equity().mortgageBalance().get());
      }
      if (pd.dashboard().summary().totalEquity().isPresent()) {
        totalEquityForRatio = totalEquityForRatio.add(pd.dashboard().summary().totalEquity().get());
      }
    }
    Optional<BigDecimal> debtToEquity =
        totalEquityForRatio.compareTo(ZERO) > 0
            ? Optional.of(totalMortgage.divide(totalEquityForRatio, MONETARY_SCALE, HALF_UP))
            : Optional.empty();

    // Portfolio DSCR: totalAnnualNoi / (12 * sum(monthlyMortgage))
    // Derive monthly mortgage from last cash flow data point's mortgage value
    BigDecimal totalMonthlyMortgage = ZERO;
    for (PropertyData pd : sameCurrency) {
      List<MonthlyDataPoint> months = pd.dashboard().cashFlow().months();
      if (!months.isEmpty()) {
        BigDecimal lastMortgage = months.getLast().mortgage();
        totalMonthlyMortgage = totalMonthlyMortgage.add(lastMortgage);
      }
    }
    BigDecimal annualDebtService = totalMonthlyMortgage.multiply(TWELVE);
    Optional<BigDecimal> portfolioDscr =
        annualDebtService.compareTo(ZERO) > 0 && hasAnyNoi
            ? Optional.of(totalAnnualNoi.divide(annualDebtService, MONETARY_SCALE, HALF_UP))
            : Optional.empty();

    // Income concentration: max(property annual income) / total annual income * 100
    // Derive annual income from annualNoi + expenses (approximation: use annualNoi as proxy)
    BigDecimal maxPropertyNoi = ZERO;
    BigDecimal totalNoi = ZERO;
    for (PropertyData pd : sameCurrency) {
      if (pd.dashboard().summary().annualNoi().isPresent()) {
        BigDecimal noi = pd.dashboard().summary().annualNoi().get().abs();
        if (noi.compareTo(maxPropertyNoi) > 0) {
          maxPropertyNoi = noi;
        }
        totalNoi = totalNoi.add(noi);
      }
    }
    Optional<BigDecimal> incomeConcentration =
        totalNoi.compareTo(ZERO) > 0
            ? Optional.of(
                maxPropertyNoi.multiply(ONE_HUNDRED).divide(totalNoi, PERCENT_SCALE, HALF_UP))
            : Optional.empty();

    // Data completeness: average of completenessPercent
    Optional<BigDecimal> dataCompleteness =
        allData.isEmpty()
            ? Optional.empty()
            : Optional.of(
                BigDecimal.valueOf(
                        allData.stream()
                            .mapToInt(pd -> pd.dashboard().dataCompleteness().completenessPercent())
                            .sum())
                    .divide(BigDecimal.valueOf(allData.size()), PERCENT_SCALE, HALF_UP));

    return new PortfolioSummary(
        hasAnyValue ? Optional.of(totalPortfolioValue) : Optional.empty(),
        hasAnyEquity ? Optional.of(totalEquity) : Optional.empty(),
        hasAnyCashFlow ? Optional.of(totalMonthlyCashFlow) : Optional.empty(),
        hasAnyNoi ? Optional.of(totalAnnualNoi) : Optional.empty(),
        weightedCapRate,
        weightedCashOnCash,
        portfolioOccupancy,
        debtToEquity,
        portfolioDscr,
        incomeConcentration,
        dataCompleteness);
  }

  private CashFlowChartData aggregateCashFlow(List<PropertyData> sameCurrencyProperties) {
    if (sameCurrencyProperties.isEmpty()) {
      return new CashFlowChartData(List.of());
    }

    // Collect all month labels in order, then sum across properties
    Map<String, BigDecimal[]> monthAggregates = new LinkedHashMap<>();

    for (PropertyData pd : sameCurrencyProperties) {
      for (MonthlyDataPoint point : pd.dashboard().cashFlow().months()) {
        monthAggregates.computeIfAbsent(
            point.month(), k -> new BigDecimal[] {ZERO, ZERO, ZERO, ZERO});
        BigDecimal[] values = monthAggregates.get(point.month());
        values[0] = values[0].add(point.income());
        values[1] = values[1].add(point.expenses());
        values[2] = values[2].add(point.mortgage());
        values[3] = values[3].add(point.net());
      }
    }

    List<MonthlyDataPoint> aggregated =
        monthAggregates.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(
                entry ->
                    new MonthlyDataPoint(
                        entry.getKey(),
                        entry.getValue()[0],
                        entry.getValue()[1],
                        entry.getValue()[2],
                        entry.getValue()[3]))
            .toList();

    return new CashFlowChartData(aggregated);
  }

  private OccupancyChartData aggregateOccupancy(List<PropertyData> allData) {
    if (allData.isEmpty()) {
      return new OccupancyChartData(List.of());
    }

    // Average tenant/self occupancy across properties per month
    Map<String, BigDecimal[]> monthTotals = new LinkedHashMap<>();
    Map<String, Integer> monthCounts = new LinkedHashMap<>();

    for (PropertyData pd : allData) {
      for (OccupancyDataPoint point : pd.dashboard().occupancy().months()) {
        monthTotals.computeIfAbsent(point.month(), k -> new BigDecimal[] {ZERO, ZERO});
        BigDecimal[] values = monthTotals.get(point.month());
        values[0] = values[0].add(point.tenantOccupancyPercent());
        values[1] = values[1].add(point.selfOccupancyPercent());
        monthCounts.merge(point.month(), 1, Integer::sum);
      }
    }

    List<OccupancyDataPoint> aggregated =
        monthTotals.entrySet().stream()
            .map(
                entry -> {
                  int count = monthCounts.getOrDefault(entry.getKey(), 1);
                  BigDecimal divisor = BigDecimal.valueOf(count);
                  return new OccupancyDataPoint(
                      entry.getKey(),
                      entry.getValue()[0].divide(divisor, PERCENT_SCALE, HALF_UP),
                      entry.getValue()[1].divide(divisor, PERCENT_SCALE, HALF_UP));
                })
            .toList();

    return new OccupancyChartData(aggregated);
  }

  private List<PropertyPerformance> buildPropertyComparison(
      List<PropertyData> allData, String defaultCurrency) {
    return allData.stream()
        .map(
            pd -> {
              Property p = pd.property();
              PropertyDashboardResponse.SummaryMetrics s = pd.dashboard().summary();
              Sid identifier = p.getIdentifier().orElseThrow();
              String address = p.getStreet() + ", " + p.getCity();
              String category = p.getPropertyCategory().name();
              boolean currencyMismatch =
                  s.currency().map(c -> !defaultCurrency.equals(c)).orElse(false);

              return new PropertyPerformance(
                  identifier,
                  address,
                  category,
                  s.monthlyCashFlow(),
                  s.annualNoi(),
                  s.capRatePercent(),
                  s.cashOnCashPercent(),
                  s.occupancyRatePercent(),
                  pd.dashboard().dataCompleteness().completenessPercent(),
                  s.currency(),
                  currencyMismatch);
            })
        .toList();
  }

  private AllocationData buildAllocation(
      List<PropertyData> allData, List<PropertyData> sameCurrency) {

    // By category: use market value if available, otherwise count
    Map<String, BigDecimal> byCategoryMap = new LinkedHashMap<>();
    for (PropertyData pd : allData) {
      String category = pd.property().getPropertyCategory().name();
      BigDecimal value = pd.dashboard().equity().currentMarketValue().orElse(BigDecimal.ONE);
      byCategoryMap.merge(category, value, BigDecimal::add);
    }
    List<AllocationSlice> byCategory = toAllocationSlices(byCategoryMap);

    // By country
    Map<String, BigDecimal> byCountryMap = new LinkedHashMap<>();
    for (PropertyData pd : allData) {
      String country = pd.property().getCountryCode();
      BigDecimal value = pd.dashboard().equity().currentMarketValue().orElse(BigDecimal.ONE);
      byCountryMap.merge(country, value, BigDecimal::add);
    }
    List<AllocationSlice> byCountry = toAllocationSlices(byCountryMap);

    return new AllocationData(byCategory, byCountry);
  }

  private List<AllocationSlice> toAllocationSlices(Map<String, BigDecimal> valueMap) {
    BigDecimal total = valueMap.values().stream().reduce(ZERO, BigDecimal::add);
    if (total.compareTo(ZERO) == 0) {
      return List.of();
    }
    return valueMap.entrySet().stream()
        .map(
            entry ->
                new AllocationSlice(
                    entry.getKey(),
                    entry.getValue().setScale(MONETARY_SCALE, HALF_UP),
                    entry.getValue().multiply(ONE_HUNDRED).divide(total, PERCENT_SCALE, HALF_UP)))
        .sorted((a, b) -> b.value().compareTo(a.value()))
        .toList();
  }

  private EquityCompositionData buildEquityComposition(
      List<PropertyData> allData, String defaultCurrency) {
    List<PropertyEquity> properties =
        allData.stream()
            .map(
                pd -> {
                  Property p = pd.property();
                  PropertyDashboardResponse.EquityChartData equity = pd.dashboard().equity();
                  Sid identifier = p.getIdentifier().orElseThrow();
                  String address = p.getStreet() + ", " + p.getCity();

                  Optional<BigDecimal> equityAmount =
                      equity
                          .currentMarketValue()
                          .map(mv -> mv.subtract(equity.mortgageBalance().orElse(ZERO)));

                  return new PropertyEquity(
                      identifier,
                      address,
                      equityAmount,
                      equity.mortgageBalance(),
                      equity.currentMarketValue());
                })
            .toList();

    return new EquityCompositionData(properties);
  }
}
