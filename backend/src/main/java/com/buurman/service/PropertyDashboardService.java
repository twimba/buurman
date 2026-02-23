package com.buurman.service;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Contract;
import com.buurman.domain.Contract.ContractStatus;
import com.buurman.domain.Expense;
import com.buurman.domain.Payment;
import com.buurman.domain.Property;
import com.buurman.dto.response.PropertyDashboardResponse;
import com.buurman.dto.response.PropertyDashboardResponse.CashFlowChartData;
import com.buurman.dto.response.PropertyDashboardResponse.CategorySlice;
import com.buurman.dto.response.PropertyDashboardResponse.DataCompleteness;
import com.buurman.dto.response.PropertyDashboardResponse.EquityChartData;
import com.buurman.dto.response.PropertyDashboardResponse.ExpenseBreakdownChartData;
import com.buurman.dto.response.PropertyDashboardResponse.ExpenseTimelineMonth;
import com.buurman.dto.response.PropertyDashboardResponse.FutureMonthDataPoint;
import com.buurman.dto.response.PropertyDashboardResponse.FutureTrendData;
import com.buurman.dto.response.PropertyDashboardResponse.MonthlyDataPoint;
import com.buurman.dto.response.PropertyDashboardResponse.OccupancyChartData;
import com.buurman.dto.response.PropertyDashboardResponse.OccupancyDataPoint;
import com.buurman.dto.response.PropertyDashboardResponse.SummaryMetrics;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.ExpenseRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PropertyDashboardService {

  private static final int SCALE = 2;
  private static final int DEFAULT_MONTHS = 12;
  public static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

  private final PropertyRepository propertyRepository;
  private final ContractRepository contractRepository;
  private final PaymentRepository paymentRepository;
  private final ExpenseRepository expenseRepository;

  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public PropertyDashboardResponse getDashboard(
      String propertyIdentifier, @Nullable Integer months, UserPrincipal principal) {
    int effectiveMonths = months != null ? months : DEFAULT_MONTHS;
    return getDashboardData(propertyIdentifier, effectiveMonths, principal.requireTeamId());
  }

  /** Internal method for use by other services (authorization handled by caller). */
  @Transactional(readOnly = true)
  public PropertyDashboardResponse getDashboardData(
      String propertyIdentifier, int months, UUID teamId) {
    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);

    List<Contract> contracts = contractRepository.findByPropertyId(property.getId(), teamId);
    List<UUID> contractIds = contracts.stream().map(Contract::getId).toList();

    String currency = property.getPurchasePriceCurrency();
    LocalDate now = LocalDate.now();

    LocalDate startDate;
    if (months <= 0) {
      // All time: fetch everything first, then derive start from earliest data
      List<Payment> unfilteredPayments =
          paymentRepository
              .findPaidByContractIdsAndDateRange(contractIds, teamId, LocalDate.of(1970, 1, 1), now)
              .stream()
              .filter(p -> currency == null || currency.equals(p.getCurrency()))
              .toList();

      List<Expense> unfilteredExpenses =
          expenseRepository.findByPropertyId(property.getId(), teamId).stream()
              .filter(e -> e.getExpenseDate() != null && !e.getExpenseDate().isAfter(now))
              .filter(e -> currency == null || currency.equals(e.getCurrency()))
              .toList();

      // Earliest date across purchase date, payments, and expenses
      LocalDate earliest = now;
      if (property.getPurchaseDate() != null) {
        earliest = property.getPurchaseDate();
      }
      for (Payment p : unfilteredPayments) {
        if (p.getPaymentDate() != null && p.getPaymentDate().isBefore(earliest)) {
          earliest = p.getPaymentDate();
        }
      }
      for (Expense e : unfilteredExpenses) {
        if (e.getExpenseDate().isBefore(earliest)) {
          earliest = e.getExpenseDate();
        }
      }
      startDate = earliest;

      // Data is already fully fetched — assign directly and skip the queries below
      return buildDashboardFromData(
          property, contracts, unfilteredPayments, unfilteredExpenses, startDate, now);
    } else {
      startDate = now.minusMonths(months);
    }

    // Fetch payments for all contracts of this property in the period (single query)
    List<Payment> allPayments =
        paymentRepository
            .findPaidByContractIdsAndDateRange(contractIds, teamId, startDate, now)
            .stream()
            .filter(p -> currency == null || currency.equals(p.getCurrency()))
            .toList();

    List<Expense> allExpenses =
        expenseRepository.findByPropertyId(property.getId(), teamId).stream()
            .filter(
                e ->
                    e.getExpenseDate() != null
                        && !e.getExpenseDate().isBefore(startDate)
                        && !e.getExpenseDate().isAfter(now))
            .filter(e -> currency == null || currency.equals(e.getCurrency()))
            .toList();

    return buildDashboardFromData(property, contracts, allPayments, allExpenses, startDate, now);
  }

  private PropertyDashboardResponse buildDashboardFromData(
      Property property,
      List<Contract> contracts,
      List<Payment> payments,
      List<Expense> expenses,
      LocalDate startDate,
      LocalDate now) {

    int effectiveMonths = (int) MONTHS.between(YearMonth.from(startDate), YearMonth.from(now)) + 1;

    SummaryMetrics summary =
        buildSummaryMetrics(property, contracts, payments, expenses, effectiveMonths);
    CashFlowChartData cashFlow =
        buildCashFlowChart(payments, expenses, property, now, effectiveMonths);
    EquityChartData equity = buildEquityChart(property);
    ExpenseBreakdownChartData expenseBreakdown =
        buildExpenseBreakdown(expenses, property, now, effectiveMonths);
    OccupancyChartData occupancy = buildOccupancyChart(contracts, now, effectiveMonths);
    DataCompleteness completeness =
        buildDataCompleteness(property, contracts, property.getTeamId());
    PropertyDashboardResponse.FutureTrendData futureTrend =
        buildFutureTrend(property, contracts, now);

    return new PropertyDashboardResponse(
        summary, cashFlow, equity, expenseBreakdown, occupancy, completeness, futureTrend);
  }

  private SummaryMetrics buildSummaryMetrics(
      Property property,
      List<Contract> contracts,
      List<Payment> payments,
      List<Expense> expenses,
      int months) {

    BigDecimal purchasePrice = property.getPurchasePrice();
    BigDecimal marketValue = property.getCurrentMarketValue();
    BigDecimal mortgageAmount = property.getMortgageAmount();
    BigDecimal monthlyMortgage = property.getMonthlyMortgagePayment();
    String currency = property.getPurchasePriceCurrency();

    BigDecimal totalIncome = sumAmounts(payments.stream().map(Payment::getAmount).toList());
    BigDecimal totalExpenses = sumAmounts(expenses.stream().map(Expense::getAmount).toList());

    // Annualize from period data
    BigDecimal annualFactor =
        months >= 12
            ? ONE
            : BigDecimal.valueOf(12).divide(BigDecimal.valueOf(Math.max(months, 1)), 4, HALF_UP);
    BigDecimal annualIncome = totalIncome.multiply(annualFactor);
    BigDecimal annualExpenses = totalExpenses.multiply(annualFactor);
    BigDecimal annualMortgage =
        monthlyMortgage != null ? monthlyMortgage.multiply(BigDecimal.valueOf(12)) : ZERO;

    // Include property-level operating costs in expenses
    BigDecimal annualOperatingCosts =
        sumAmounts(
            java.util.Arrays.asList(
                property.getAnnualPropertyTax(),
                property.getAnnualInsurance(),
                property.getAnnualHoaFee(),
                property.getAnnualManagementFee(),
                property.getAnnualMaintenanceReserve()));

    // Annual NOI = income - recorded expenses - property-level operating costs (excluding mortgage)
    BigDecimal annualNoi = annualIncome.subtract(annualExpenses).subtract(annualOperatingCosts);

    // Monthly Cash Flow = avg monthly income - avg monthly expenses - operating costs/12 - mortgage
    Optional<BigDecimal> avgMonthlyIncome =
        divideOrNull(totalIncome, BigDecimal.valueOf(Math.max(months, 1)));
    Optional<BigDecimal> avgMonthlyExpenses =
        divideOrNull(totalExpenses, BigDecimal.valueOf(Math.max(months, 1)));
    BigDecimal monthlyOperatingCosts =
        annualOperatingCosts.divide(BigDecimal.valueOf(12), SCALE, HALF_UP);
    BigDecimal monthlyCashFlow =
        avgMonthlyIncome
            .map(
                income ->
                    income
                        .subtract(avgMonthlyExpenses.orElse(ZERO))
                        .subtract(monthlyOperatingCosts)
                        .subtract(monthlyMortgage != null ? monthlyMortgage : ZERO))
            .orElse(null);

    // Total Equity = market value - mortgage balance
    BigDecimal totalEquity = null;
    if (marketValue != null) {
      totalEquity = marketValue.subtract(mortgageAmount != null ? mortgageAmount : ZERO);
    }

    // Equity Growth % = (marketValue - purchasePrice) / purchasePrice * 100
    BigDecimal equityGrowthPercent = percentChange(purchasePrice, marketValue).orElse(null);

    // Total ROI = (marketValue - purchasePrice + netIncome) / purchasePrice * 100
    BigDecimal totalRoiPercent = null;
    if (purchasePrice != null && purchasePrice.compareTo(ZERO) > 0 && marketValue != null) {
      BigDecimal netIncome = annualNoi.subtract(annualMortgage);
      totalRoiPercent =
          marketValue
              .subtract(purchasePrice)
              .add(netIncome)
              .multiply(ONE_HUNDRED)
              .divide(purchasePrice, SCALE, HALF_UP);
    }

    // Annualized ROI = totalROI / yearsOwned
    BigDecimal annualizedRoiPercent = null;
    if (totalRoiPercent != null && property.getPurchaseDate() != null) {
      long daysOwned = DAYS.between(property.getPurchaseDate(), LocalDate.now());
      if (daysOwned > 0) {
        BigDecimal yearsOwned =
            BigDecimal.valueOf(daysOwned).divide(BigDecimal.valueOf(365.25), 4, HALF_UP);
        if (yearsOwned.compareTo(ZERO) > 0) {
          annualizedRoiPercent = totalRoiPercent.divide(yearsOwned, SCALE, HALF_UP);
        }
      }
    }

    // Cap Rate = annual NOI / current market value * 100
    BigDecimal capRatePercent = null;
    if (marketValue != null && marketValue.compareTo(ZERO) > 0) {
      capRatePercent = annualNoi.multiply(ONE_HUNDRED).divide(marketValue, SCALE, HALF_UP);
    }

    // Cash-on-Cash = annual cash flow / cash invested * 100
    // Cash invested = purchase price - mortgage (i.e. down payment)
    BigDecimal cashOnCashPercent = null;
    if (purchasePrice != null && purchasePrice.compareTo(ZERO) > 0) {
      BigDecimal cashInvested =
          mortgageAmount != null ? purchasePrice.subtract(mortgageAmount) : purchasePrice;
      if (cashInvested.compareTo(ZERO) > 0) {
        BigDecimal annualCashFlow = annualNoi.subtract(annualMortgage);
        cashOnCashPercent =
            annualCashFlow.multiply(ONE_HUNDRED).divide(cashInvested, SCALE, HALF_UP);
      }
    }

    // Occupancy Rate over the selected period
    BigDecimal occupancyRatePercent =
        calculateOccupancyRate(contracts, LocalDate.now(), months).orElse(null);

    // Gross Rent Multiplier = market value / annual gross rent
    BigDecimal grossRentMultiplier = null;
    if (marketValue != null && annualIncome.compareTo(ZERO) > 0) {
      grossRentMultiplier = marketValue.divide(annualIncome, SCALE, HALF_UP);
    }

    return new SummaryMetrics(
        Optional.ofNullable(totalRoiPercent),
        Optional.ofNullable(annualizedRoiPercent),
        Optional.ofNullable(capRatePercent),
        Optional.ofNullable(cashOnCashPercent),
        Optional.ofNullable(monthlyCashFlow),
        Optional.ofNullable(annualNoi),
        Optional.ofNullable(totalEquity),
        Optional.ofNullable(equityGrowthPercent),
        Optional.ofNullable(occupancyRatePercent),
        Optional.ofNullable(grossRentMultiplier),
        Optional.ofNullable(currency));
  }

  private CashFlowChartData buildCashFlowChart(
      List<Payment> payments,
      List<Expense> expenses,
      Property property,
      LocalDate now,
      int months) {
    BigDecimal monthlyMortgage = property.getMonthlyMortgagePayment();

    // Build per-month operating cost map (due-month-aware)
    Map<Integer, BigDecimal> operatingCostsByMonth = new java.util.HashMap<>();
    addAnnualCostToMonth(
        operatingCostsByMonth,
        property.getAnnualPropertyTax(),
        property.getAnnualPropertyTaxDueMonth());
    addAnnualCostToMonth(
        operatingCostsByMonth,
        property.getAnnualInsurance(),
        property.getAnnualInsuranceDueMonth());
    addAnnualCostToMonth(
        operatingCostsByMonth, property.getAnnualHoaFee(), property.getAnnualHoaFeeDueMonth());
    addAnnualCostToMonth(
        operatingCostsByMonth,
        property.getAnnualManagementFee(),
        property.getAnnualManagementFeeDueMonth());
    addAnnualCostToMonth(
        operatingCostsByMonth,
        property.getAnnualMaintenanceReserve(),
        property.getAnnualMaintenanceReserveDueMonth());

    Map<YearMonth, BigDecimal> incomeByMonth =
        payments.stream()
            .filter(p -> p.getPaymentDate() != null)
            .collect(
                Collectors.groupingBy(
                    p -> YearMonth.from(p.getPaymentDate()),
                    Collectors.reducing(ZERO, Payment::getAmount, BigDecimal::add)));

    Map<YearMonth, BigDecimal> expensesByMonth =
        expenses.stream()
            .filter(e -> e.getExpenseDate() != null)
            .collect(
                Collectors.groupingBy(
                    e -> YearMonth.from(e.getExpenseDate()),
                    Collectors.reducing(ZERO, Expense::getAmount, BigDecimal::add)));

    List<MonthlyDataPoint> dataPoints = new ArrayList<>();
    for (int i = months - 1; i >= 0; i--) {
      YearMonth ym = YearMonth.from(now.minusMonths(i));
      BigDecimal income = incomeByMonth.getOrDefault(ym, ZERO);
      BigDecimal opCosts = operatingCostsByMonth.getOrDefault(ym.getMonthValue(), ZERO);
      BigDecimal exp = expensesByMonth.getOrDefault(ym, ZERO).add(opCosts);
      BigDecimal mort = monthlyMortgage != null ? monthlyMortgage : ZERO;
      BigDecimal net = income.subtract(exp).subtract(mort);
      dataPoints.add(new MonthlyDataPoint(ym.toString(), income, exp, mort, net));
    }
    return new CashFlowChartData(dataPoints);
  }

  private EquityChartData buildEquityChart(Property property) {
    return new EquityChartData(
        Optional.ofNullable(property.getPurchasePrice()),
        Optional.ofNullable(property.getCurrentMarketValue()),
        Optional.ofNullable(property.getMortgageAmount()));
  }

  private ExpenseBreakdownChartData buildExpenseBreakdown(
      List<Expense> expenses, Property property, LocalDate now, int months) {
    Map<String, BigDecimal> byCategory =
        new java.util.LinkedHashMap<>(
            expenses.stream()
                .collect(
                    Collectors.groupingBy(
                        e -> e.getCategory() != null ? e.getCategory().name() : "OTHER",
                        Collectors.reducing(ZERO, Expense::getAmount, BigDecimal::add))));

    // Add property-level annual operating costs to breakdown.
    // MANAGEMENT and MAINTENANCE_RESERVE are excluded — they are budget allocations,
    // not actual incurred expenses recorded against the property.
    addIfNotNull(byCategory, "PROPERTY_TAX", property.getAnnualPropertyTax());
    addIfNotNull(byCategory, "INSURANCE", property.getAnnualInsurance());
    addIfNotNull(byCategory, "HOA", property.getAnnualHoaFee());

    List<CategorySlice> slices =
        byCategory.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .map(e -> new CategorySlice(e.getKey(), e.getValue()))
            .toList();

    // Build monthly timeline grouped by category
    YearMonth startMonth = YearMonth.from(now.minusMonths(months - 1));
    YearMonth endMonth = YearMonth.from(now);

    // Group recorded expenses by month + category
    Map<YearMonth, Map<String, BigDecimal>> monthlyMap = new java.util.LinkedHashMap<>();
    for (YearMonth ym = startMonth; !ym.isAfter(endMonth); ym = ym.plusMonths(1)) {
      monthlyMap.put(ym, new java.util.LinkedHashMap<>());
    }
    for (Expense e : expenses) {
      YearMonth ym = YearMonth.from(e.getExpenseDate());
      Map<String, BigDecimal> monthMap = monthlyMap.get(ym);
      if (monthMap != null) {
        String cat = e.getCategory() != null ? e.getCategory().name() : "OTHER";
        monthMap.merge(cat, e.getAmount(), BigDecimal::add);
      }
    }

    // Distribute property-level annual costs (due-month-aware)
    addAnnualCostToTimeline(
        monthlyMap,
        "PROPERTY_TAX",
        property.getAnnualPropertyTax(),
        property.getAnnualPropertyTaxDueMonth());
    addAnnualCostToTimeline(
        monthlyMap,
        "INSURANCE",
        property.getAnnualInsurance(),
        property.getAnnualInsuranceDueMonth());
    addAnnualCostToTimeline(
        monthlyMap, "HOA", property.getAnnualHoaFee(), property.getAnnualHoaFeeDueMonth());

    List<ExpenseTimelineMonth> timeline =
        monthlyMap.entrySet().stream()
            .map(entry -> new ExpenseTimelineMonth(entry.getKey().toString(), entry.getValue()))
            .toList();

    return new ExpenseBreakdownChartData(slices, timeline);
  }

  /**
   * Parses a comma-separated month string (e.g. "1,3,7") into a list of month numbers. Returns null
   * if the input is null or blank (meaning all months).
   */
  private static @Nullable List<Integer> parseDueMonths(@Nullable String dueMonths) {
    if (dueMonths == null || dueMonths.isBlank()) {
      return null;
    }
    return java.util.Arrays.stream(dueMonths.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(Integer::parseInt)
        .toList();
  }

  /**
   * Distributes an annual cost into a month-indexed map. If dueMonths is set, the amount is divided
   * among those months. Otherwise, it's spread evenly across all 12 months.
   */
  private static void addAnnualCostToMonth(
      Map<Integer, BigDecimal> monthMap,
      @Nullable BigDecimal annualAmount,
      @Nullable String dueMonths) {
    if (annualAmount == null || annualAmount.compareTo(ZERO) <= 0) {
      return;
    }
    List<Integer> months = parseDueMonths(dueMonths);
    if (months != null && !months.isEmpty()) {
      BigDecimal perMonth = annualAmount.divide(BigDecimal.valueOf(months.size()), SCALE, HALF_UP);
      for (int m : months) {
        monthMap.merge(m, perMonth, BigDecimal::add);
      }
    } else {
      BigDecimal monthly = annualAmount.divide(BigDecimal.valueOf(12), SCALE, HALF_UP);
      for (int m = 1; m <= 12; m++) {
        monthMap.merge(m, monthly, BigDecimal::add);
      }
    }
  }

  /**
   * Adds an annual cost to the expense timeline. If dueMonths is set, the amount is divided among
   * matching months. Otherwise, it's spread evenly (÷12) across all months.
   */
  private static void addAnnualCostToTimeline(
      Map<YearMonth, Map<String, BigDecimal>> timeline,
      String category,
      @Nullable BigDecimal annualAmount,
      @Nullable String dueMonths) {
    if (annualAmount == null || annualAmount.compareTo(ZERO) <= 0) {
      return;
    }
    List<Integer> months = parseDueMonths(dueMonths);
    if (months != null && !months.isEmpty()) {
      BigDecimal perMonth = annualAmount.divide(BigDecimal.valueOf(months.size()), SCALE, HALF_UP);
      for (Map.Entry<YearMonth, Map<String, BigDecimal>> entry : timeline.entrySet()) {
        if (months.contains(entry.getKey().getMonthValue())) {
          entry.getValue().merge(category, perMonth, BigDecimal::add);
        }
      }
    } else {
      BigDecimal monthly = annualAmount.divide(BigDecimal.valueOf(12), SCALE, HALF_UP);
      for (Map<String, BigDecimal> monthData : timeline.values()) {
        monthData.merge(category, monthly, BigDecimal::add);
      }
    }
  }

  private static void addIfNotNull(
      Map<String, BigDecimal> map, String key, @Nullable BigDecimal value) {
    if (value != null && value.compareTo(ZERO) > 0) {
      map.merge(key, value, BigDecimal::add);
    }
  }

  private OccupancyChartData buildOccupancyChart(
      List<Contract> contracts, LocalDate now, int months) {
    // Include ACTIVE, EXPIRED, and TERMINATED — all represent periods of actual occupancy.
    // DRAFT and PENDING_SIGNATURE are excluded since the tenant hasn't moved in yet.
    List<Contract> occupiedContracts =
        contracts.stream()
            .filter(
                c ->
                    c.getStatus() == ContractStatus.ACTIVE
                        || c.getStatus() == ContractStatus.EXPIRED
                        || c.getStatus() == ContractStatus.TERMINATED)
            .toList();

    List<OccupancyDataPoint> dataPoints = new ArrayList<>();
    for (int i = months - 1; i >= 0; i--) {
      YearMonth ym = YearMonth.from(now.minusMonths(i));
      LocalDate monthStart = ym.atDay(1);
      LocalDate monthEnd = ym.atEndOfMonth();
      int daysInMonth = ym.lengthOfMonth();

      long occupiedDays = 0;
      for (Contract c : occupiedContracts) {
        if (c.getStartDate() == null) {
          continue;
        }
        LocalDate cStart = c.getStartDate().isBefore(monthStart) ? monthStart : c.getStartDate();
        LocalDate cEnd =
            (c.getEndDate() == null || c.getEndDate().isAfter(monthEnd))
                ? monthEnd
                : c.getEndDate();
        if (!cStart.isAfter(cEnd)) {
          occupiedDays += DAYS.between(cStart, cEnd) + 1;
        }
      }
      // Cap at days in month (overlapping contracts shouldn't exceed 100%)
      occupiedDays = Math.min(occupiedDays, daysInMonth);
      BigDecimal pct =
          BigDecimal.valueOf(occupiedDays)
              .multiply(ONE_HUNDRED)
              .divide(BigDecimal.valueOf(daysInMonth), SCALE, HALF_UP);
      dataPoints.add(new OccupancyDataPoint(ym.toString(), pct));
    }
    return new OccupancyChartData(dataPoints);
  }

  private FutureTrendData buildFutureTrend(
      Property property, List<Contract> contracts, LocalDate now) {
    List<FutureMonthDataPoint> points = new ArrayList<>();

    for (int i = 1; i <= 6; i++) {
      YearMonth futureMonth = YearMonth.from(now).plusMonths(i);
      LocalDate monthStart = futureMonth.atDay(1);
      LocalDate monthEnd = futureMonth.atEndOfMonth();
      int monthNumber = futureMonth.getMonthValue();

      // Expected income: sum of monthly rent from active contracts covering this month
      BigDecimal expectedIncome =
          contracts.stream()
              .filter(
                  c ->
                      c.getStatus() == ContractStatus.ACTIVE
                          && !c.getStartDate().isAfter(monthEnd)
                          && (c.getEndDate() == null || !c.getEndDate().isBefore(monthStart)))
              .map(c -> c.getRentAmount() != null ? c.getRentAmount() : ZERO)
              .reduce(ZERO, BigDecimal::add);

      // Expected expenses: operating costs due in this month + monthly mortgage
      BigDecimal expectedExpenses = ZERO;

      // Monthly mortgage payment
      if (property.getMonthlyMortgagePayment() != null
          && property.getMonthlyMortgagePayment().compareTo(ZERO) > 0) {
        expectedExpenses = expectedExpenses.add(property.getMonthlyMortgagePayment());
      }

      // Add annual costs that are due in this month
      expectedExpenses =
          expectedExpenses.add(
              getMonthlyShareOfAnnualCost(
                  property.getAnnualPropertyTax(),
                  property.getAnnualPropertyTaxDueMonth(),
                  monthNumber));
      expectedExpenses =
          expectedExpenses.add(
              getMonthlyShareOfAnnualCost(
                  property.getAnnualInsurance(),
                  property.getAnnualInsuranceDueMonth(),
                  monthNumber));
      expectedExpenses =
          expectedExpenses.add(
              getMonthlyShareOfAnnualCost(
                  property.getAnnualHoaFee(), property.getAnnualHoaFeeDueMonth(), monthNumber));
      expectedExpenses =
          expectedExpenses.add(
              getMonthlyShareOfAnnualCost(
                  property.getAnnualManagementFee(),
                  property.getAnnualManagementFeeDueMonth(),
                  monthNumber));
      expectedExpenses =
          expectedExpenses.add(
              getMonthlyShareOfAnnualCost(
                  property.getAnnualMaintenanceReserve(),
                  property.getAnnualMaintenanceReserveDueMonth(),
                  monthNumber));

      BigDecimal expectedNet = expectedIncome.subtract(expectedExpenses);
      points.add(
          new FutureMonthDataPoint(
              futureMonth.toString(), expectedIncome, expectedExpenses, expectedNet));
    }

    return new FutureTrendData(points);
  }

  /**
   * Returns the monthly share of an annual cost if it falls due in the given month. dueMonths is a
   * comma-separated list of month numbers (1-12). The annual cost is divided evenly among the due
   * months.
   */
  private BigDecimal getMonthlyShareOfAnnualCost(
      @Nullable BigDecimal annualAmount, @Nullable String dueMonths, int currentMonth) {
    if (annualAmount == null || annualAmount.compareTo(ZERO) <= 0) {
      return ZERO;
    }
    if (dueMonths == null || dueMonths.isBlank()) {
      // If no due months specified, spread evenly across 12 months
      return annualAmount.divide(BigDecimal.valueOf(12), SCALE, HALF_UP);
    }
    @Nullable List<Integer> months = parseDueMonths(dueMonths);
    if (months == null || months.isEmpty()) {
      return annualAmount.divide(BigDecimal.valueOf(12), SCALE, HALF_UP);
    }
    if (!months.contains(currentMonth)) {
      return ZERO;
    }
    return annualAmount.divide(BigDecimal.valueOf(months.size()), SCALE, HALF_UP);
  }

  private DataCompleteness buildDataCompleteness(
      Property property, List<Contract> contracts, UUID teamId) {

    boolean hasPurchasePrice = property.getPurchasePrice() != null;
    boolean hasMarketValue = property.getCurrentMarketValue() != null;
    boolean hasMortgageInfo = property.getMortgageType() != null;
    boolean hasOperatingCosts =
        property.getAnnualPropertyTax() != null || property.getAnnualInsurance() != null;
    boolean hasContracts = !contracts.isEmpty();
    // Use all-time checks for payments/expenses (independent of period filter)
    List<UUID> contractIds = contracts.stream().map(Contract::getId).toList();
    boolean hasPayments =
        !contractIds.isEmpty()
            && !paymentRepository
                .findPaidByContractIdsAndDateRange(
                    contractIds, teamId, LocalDate.of(1970, 1, 1), LocalDate.now())
                .isEmpty();
    boolean hasExpenses = !expenseRepository.findByPropertyId(property.getId(), teamId).isEmpty();

    int filled = 0;
    int total = 7;

    filled += hasPurchasePrice ? 1 : 0;
    filled += hasMarketValue ? 1 : 0;
    filled += hasMortgageInfo ? 1 : 0;
    filled += hasOperatingCosts ? 1 : 0;
    filled += hasContracts ? 1 : 0;
    filled += hasPayments ? 1 : 0;
    filled += hasExpenses ? 1 : 0;

    int percent = (int) Math.round(filled * 100.0 / total);

    return new DataCompleteness(
        hasPurchasePrice,
        hasMarketValue,
        hasMortgageInfo,
        hasOperatingCosts,
        hasContracts,
        hasPayments,
        hasExpenses,
        percent);
  }

  private Optional<BigDecimal> calculateOccupancyRate(
      List<Contract> contracts, LocalDate now, int months) {
    LocalDate start = now.minusMonths(months);
    long totalDays = DAYS.between(start, now);
    if (totalDays <= 0) {
      return Optional.empty();
    }

    long occupiedDays = 0;
    for (Contract c : contracts) {
      if (c.getStartDate() == null) {
        continue;
      }
      if (c.getStatus() != ContractStatus.ACTIVE
          && c.getStatus() != ContractStatus.EXPIRED
          && c.getStatus() != ContractStatus.TERMINATED) {
        continue;
      }

      LocalDate cStart = c.getStartDate().isBefore(start) ? start : c.getStartDate();
      LocalDate cEnd =
          (c.getEndDate() == null || c.getEndDate().isAfter(now)) ? now : c.getEndDate();
      if (!cStart.isAfter(cEnd)) {
        occupiedDays += DAYS.between(cStart, cEnd) + 1;
      }
    }
    occupiedDays = Math.min(occupiedDays, totalDays);

    return Optional.of(
        BigDecimal.valueOf(occupiedDays)
            .multiply(ONE_HUNDRED)
            .divide(BigDecimal.valueOf(totalDays), SCALE, HALF_UP));
  }

  private static BigDecimal sumAmounts(List<BigDecimal> amounts) {
    return amounts.stream().filter(java.util.Objects::nonNull).reduce(ZERO, BigDecimal::add);
  }

  private static Optional<BigDecimal> divideOrNull(
      @Nullable BigDecimal numerator, BigDecimal denominator) {
    if (numerator == null || denominator == null || denominator.compareTo(ZERO) == 0) {
      return Optional.empty();
    }
    return Optional.of(numerator.divide(denominator, SCALE, HALF_UP));
  }

  private static Optional<BigDecimal> percentChange(
      @Nullable BigDecimal from, @Nullable BigDecimal to) {
    if (from == null || to == null || from.compareTo(ZERO) == 0) {
      return Optional.empty();
    }
    return Optional.of(to.subtract(from).multiply(ONE_HUNDRED).divide(from, SCALE, HALF_UP));
  }
}
