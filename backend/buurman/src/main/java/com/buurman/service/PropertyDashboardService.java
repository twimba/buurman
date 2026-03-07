package com.buurman.service;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;

import java.math.BigDecimal;
import java.time.Clock;
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
import com.buurman.domain.FinancingPayment;
import com.buurman.domain.Payment;
import com.buurman.domain.Property;
import com.buurman.domain.PropertyAcquisition;
import com.buurman.domain.PropertyOccupancyPeriod;
import com.buurman.domain.Sid;
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
import com.buurman.repository.FinancingPaymentRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyAcquisitionRepository;
import com.buurman.repository.PropertyOccupancyPeriodRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;
import com.buurman.util.MoneyAmount;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PropertyDashboardService {

  private static final int SCALE = 2;
  private static final int DEFAULT_MONTHS = 12;
  public static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

  private final Clock clock;
  private final PropertyRepository propertyRepository;
  private final ContractRepository contractRepository;
  private final PaymentRepository paymentRepository;
  private final ExpenseRepository expenseRepository;
  private final FinancingPaymentRepository financingPaymentRepository;
  private final PropertyFinancialsService financialsService;
  private final PropertyAcquisitionRepository acquisitionRepository;
  private final PropertyOccupancyPeriodRepository occupancyPeriodRepository;

  /**
   * Pre-fetched financial data from new normalized tables. Loaded once per dashboard request and
   * passed through all build methods to avoid repeated queries.
   */
  private record FinancialData(
      Optional<BigDecimal> purchasePrice,
      Optional<String> purchasePriceCurrency,
      Optional<LocalDate> purchaseDate,
      Optional<BigDecimal> marketValue,
      Optional<BigDecimal> financingBalance,
      Optional<BigDecimal> monthlyFinancingPayment,
      boolean hasVariablePayment,
      BigDecimal annualOperatingCosts,
      Map<Integer, BigDecimal> operatingCostsByMonth) {}

  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public PropertyDashboardResponse getDashboard(
      Sid propertyIdentifier, @Nullable Integer months, UserPrincipal principal) {
    int effectiveMonths = months != null ? months : DEFAULT_MONTHS;
    return getDashboardData(propertyIdentifier, effectiveMonths, principal.requireTeamId());
  }

  /** Internal method for use by other services (authorization handled by caller). */
  @Transactional(readOnly = true)
  public PropertyDashboardResponse getDashboardData(
      Sid propertyIdentifier, int months, UUID teamId) {
    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);
    UUID propertyId = property.getId();

    // Load financial data from new normalized tables
    FinancialData financialData = loadFinancialData(propertyId, teamId);

    List<Contract> contracts = contractRepository.findByPropertyId(propertyId, teamId);
    List<UUID> contractIds = contracts.stream().map(Contract::getId).toList();
    List<PropertyOccupancyPeriod> occupancyPeriods =
        occupancyPeriodRepository.findByPropertyIdAndTeamId(property.getId(), teamId);

    String currency = financialData.purchasePriceCurrency().orElse(null);
    LocalDate now = LocalDate.now(clock);

    LocalDate startDate;
    if (months <= 0) {
      List<Payment> unfilteredPayments =
          paymentRepository
              .findPaidByContractIdsAndDateRange(contractIds, teamId, LocalDate.of(1970, 1, 1), now)
              .stream()
              .filter(p -> currency == null || currency.equals(p.getAmount().currency()))
              .toList();

      List<Expense> unfilteredExpenses =
          expenseRepository.findByPropertyId(propertyId, teamId).stream()
              .filter(e -> e.getExpenseDate() != null && !e.getExpenseDate().isAfter(now))
              .filter(e -> currency == null || currency.equals(e.getAmount().currency()))
              .toList();

      LocalDate earliest = now;
      if (financialData.purchaseDate().isPresent()) {
        earliest = financialData.purchaseDate().get();
      }
      for (Payment p : unfilteredPayments) {
        if (p.getPaymentDate().isPresent() && p.getPaymentDate().get().isBefore(earliest)) {
          earliest = p.getPaymentDate().get();
        }
      }
      for (Expense e : unfilteredExpenses) {
        if (e.getExpenseDate().isBefore(earliest)) {
          earliest = e.getExpenseDate();
        }
      }
      for (PropertyOccupancyPeriod p : occupancyPeriods) {
        if (p.getStartDate().isBefore(earliest)) {
          earliest = p.getStartDate();
        }
      }
      startDate = earliest;

      List<FinancingPayment> unfilteredFinancingPayments =
          financingPaymentRepository.findByPropertyIdAndTeamId(propertyId, teamId).stream()
              .filter(fp -> !fp.getPaymentDate().isAfter(now))
              .filter(fp -> currency == null || currency.equals(fp.getTotalAmount().currency()))
              .toList();

      return buildDashboardFromData(
          property,
          financialData,
          contracts,
          occupancyPeriods,
          unfilteredPayments,
          unfilteredExpenses,
          unfilteredFinancingPayments,
          startDate,
          now);
    } else {
      startDate = now.minusMonths(months);
    }

    List<Payment> allPayments =
        paymentRepository
            .findPaidByContractIdsAndDateRange(contractIds, teamId, startDate, now)
            .stream()
            .filter(p -> currency == null || currency.equals(p.getAmount().currency()))
            .toList();

    List<Expense> allExpenses =
        expenseRepository.findByPropertyId(propertyId, teamId).stream()
            .filter(
                e ->
                    e.getExpenseDate() != null
                        && !e.getExpenseDate().isBefore(startDate)
                        && !e.getExpenseDate().isAfter(now))
            .filter(e -> currency == null || currency.equals(e.getAmount().currency()))
            .toList();

    List<FinancingPayment> allFinancingPayments =
        financingPaymentRepository.findByPropertyIdAndTeamId(propertyId, teamId).stream()
            .filter(
                fp -> !fp.getPaymentDate().isBefore(startDate) && !fp.getPaymentDate().isAfter(now))
            .filter(fp -> currency == null || currency.equals(fp.getTotalAmount().currency()))
            .toList();

    return buildDashboardFromData(
        property,
        financialData,
        contracts,
        occupancyPeriods,
        allPayments,
        allExpenses,
        allFinancingPayments,
        startDate,
        now);
  }

  private FinancialData loadFinancialData(UUID propertyId, UUID teamId) {
    Optional<PropertyAcquisition> acquisition =
        acquisitionRepository.findByPropertyIdAndTeamId(propertyId, teamId);

    Optional<MoneyAmount> purchasePriceMoney =
        acquisition.flatMap(PropertyAcquisition::getPurchasePrice);
    Optional<BigDecimal> purchasePrice = purchasePriceMoney.map(MoneyAmount::value);
    Optional<String> purchasePriceCurrency = purchasePriceMoney.map(MoneyAmount::currency);
    Optional<LocalDate> purchaseDate = acquisition.flatMap(PropertyAcquisition::getAcquisitionDate);

    Optional<BigDecimal> marketValue =
        financialsService.getLatestValuationAmount(propertyId, teamId);
    Optional<BigDecimal> financingBalance =
        financialsService.sumActiveFinancingBalances(propertyId, teamId);
    Optional<BigDecimal> monthlyFinancingPayment =
        financialsService.getTotalMonthlyFinancingPayment(propertyId, teamId);
    boolean hasVariablePayment = financialsService.hasVariablePaymentFinancing(propertyId, teamId);

    // Operating costs from taxes + insurance + fees
    Optional<BigDecimal> annualTaxes = financialsService.sumActiveAnnualTaxes(propertyId, teamId);
    Optional<BigDecimal> annualInsurance =
        financialsService.sumActiveAnnualInsurance(propertyId, teamId);
    Optional<BigDecimal> annualFees = financialsService.sumActiveAnnualFees(propertyId, teamId);

    BigDecimal annualOperatingCosts =
        annualTaxes.orElse(ZERO).add(annualInsurance.orElse(ZERO)).add(annualFees.orElse(ZERO));

    Map<Integer, BigDecimal> operatingCostsByMonth =
        financialsService.getMonthlyOperatingCosts(propertyId, teamId);

    return new FinancialData(
        purchasePrice,
        purchasePriceCurrency,
        purchaseDate,
        marketValue,
        financingBalance,
        monthlyFinancingPayment,
        hasVariablePayment,
        annualOperatingCosts,
        operatingCostsByMonth);
  }

  private PropertyDashboardResponse buildDashboardFromData(
      Property property,
      FinancialData financialData,
      List<Contract> contracts,
      List<PropertyOccupancyPeriod> occupancyPeriods,
      List<Payment> payments,
      List<Expense> expenses,
      List<FinancingPayment> financingPayments,
      LocalDate startDate,
      LocalDate now) {

    int effectiveMonths = (int) MONTHS.between(YearMonth.from(startDate), YearMonth.from(now)) + 1;

    SummaryMetrics summary =
        buildSummaryMetrics(financialData, contracts, payments, expenses, effectiveMonths);
    CashFlowChartData cashFlow =
        buildCashFlowChart(
            payments, expenses, financingPayments, financialData, now, effectiveMonths);
    EquityChartData equity = buildEquityChart(financialData);
    ExpenseBreakdownChartData expenseBreakdown =
        buildExpenseBreakdown(expenses, financingPayments, financialData, now, effectiveMonths);
    OccupancyChartData occupancy =
        buildOccupancyChart(contracts, occupancyPeriods, now, effectiveMonths);
    DataCompleteness completeness =
        buildDataCompleteness(financialData, contracts, property.getId(), property.getTeamId());
    FutureTrendData futureTrend = buildFutureTrend(financialData, contracts, now);

    return new PropertyDashboardResponse(
        summary, cashFlow, equity, expenseBreakdown, occupancy, completeness, futureTrend);
  }

  private SummaryMetrics buildSummaryMetrics(
      FinancialData fd,
      List<Contract> contracts,
      List<Payment> payments,
      List<Expense> expenses,
      int months) {

    BigDecimal purchasePrice = fd.purchasePrice().orElse(null);
    BigDecimal marketValue = fd.marketValue().orElse(null);
    BigDecimal mortgageAmount = fd.financingBalance().orElse(null);
    BigDecimal monthlyMortgage =
        fd.hasVariablePayment() ? null : fd.monthlyFinancingPayment().orElse(null);
    String currency = fd.purchasePriceCurrency().orElse(null);

    BigDecimal totalIncome = sumAmounts(payments.stream().map(p -> p.getAmount().value()).toList());
    BigDecimal totalExpenses =
        sumAmounts(expenses.stream().map(e -> e.getAmount().value()).toList());

    // Annualize from period data
    BigDecimal annualFactor =
        months >= 12
            ? ONE
            : BigDecimal.valueOf(12).divide(BigDecimal.valueOf(Math.max(months, 1)), 4, HALF_UP);
    BigDecimal annualIncome = totalIncome.multiply(annualFactor);
    BigDecimal annualExpenses = totalExpenses.multiply(annualFactor);
    BigDecimal annualMortgage =
        monthlyMortgage != null ? monthlyMortgage.multiply(BigDecimal.valueOf(12)) : ZERO;

    BigDecimal annualOperatingCosts = fd.annualOperatingCosts();

    // Annual NOI = income - recorded expenses - operating costs (excluding mortgage)
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

    // Total Equity = market value - financing balance
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
    if (totalRoiPercent != null && fd.purchaseDate().isPresent()) {
      long daysOwned = DAYS.between(fd.purchaseDate().get(), LocalDate.now(clock));
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
    // Cash invested = purchase price - financing (i.e. down payment)
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
        calculateOccupancyRate(contracts, LocalDate.now(clock), months).orElse(null);

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
      List<FinancingPayment> financingPayments,
      FinancialData fd,
      LocalDate now,
      int months) {
    BigDecimal monthlyMortgage =
        fd.hasVariablePayment() ? null : fd.monthlyFinancingPayment().orElse(null);

    Map<Integer, BigDecimal> operatingCostsByMonth = fd.operatingCostsByMonth();

    Map<YearMonth, BigDecimal> incomeByMonth =
        payments.stream()
            .filter(p -> p.getPaymentDate().isPresent())
            .collect(
                Collectors.groupingBy(
                    p -> YearMonth.from(p.getPaymentDate().get()),
                    Collectors.reducing(ZERO, p -> p.getAmount().value(), BigDecimal::add)));

    Map<YearMonth, BigDecimal> expensesByMonth =
        expenses.stream()
            .filter(e -> e.getExpenseDate() != null)
            .collect(
                Collectors.groupingBy(
                    e -> YearMonth.from(e.getExpenseDate()),
                    Collectors.reducing(ZERO, e -> e.getAmount().value(), BigDecimal::add)));

    // Actual financing payments grouped by month (for past months)
    Map<YearMonth, BigDecimal> financingPaymentsByMonth =
        financingPayments.stream()
            .collect(
                Collectors.groupingBy(
                    fp -> YearMonth.from(fp.getPaymentDate()),
                    Collectors.reducing(ZERO, fp -> fp.getTotalAmount().value(), BigDecimal::add)));

    YearMonth currentYm = YearMonth.from(now);
    List<MonthlyDataPoint> dataPoints = new ArrayList<>();
    for (int i = months - 1; i >= 0; i--) {
      YearMonth ym = YearMonth.from(now.minusMonths(i));
      BigDecimal income = incomeByMonth.getOrDefault(ym, ZERO);

      // Operating costs from new tables only apply to future months (>= current month)
      BigDecimal opCosts =
          !ym.isBefore(currentYm)
              ? operatingCostsByMonth.getOrDefault(ym.getMonthValue(), ZERO)
              : ZERO;
      BigDecimal exp = expensesByMonth.getOrDefault(ym, ZERO).add(opCosts);

      // Past months: actual financing payments; future months: registered monthly payment
      BigDecimal mort;
      if (ym.isBefore(currentYm)) {
        mort = financingPaymentsByMonth.getOrDefault(ym, ZERO);
      } else {
        mort = monthlyMortgage != null ? monthlyMortgage : ZERO;
      }

      BigDecimal net = income.subtract(exp).subtract(mort);
      dataPoints.add(new MonthlyDataPoint(ym.toString(), income, exp, mort, net));
    }
    return new CashFlowChartData(dataPoints);
  }

  private EquityChartData buildEquityChart(FinancialData fd) {
    return new EquityChartData(fd.purchasePrice(), fd.marketValue(), fd.financingBalance());
  }

  private ExpenseBreakdownChartData buildExpenseBreakdown(
      List<Expense> expenses,
      List<FinancingPayment> financingPayments,
      FinancialData fd,
      LocalDate now,
      int months) {
    YearMonth startMonth = YearMonth.from(now.minusMonths(months - 1));
    YearMonth endMonth = YearMonth.from(now);
    YearMonth cutoff = YearMonth.from(now);

    // 1. Build monthly timeline from recorded expenses
    Map<YearMonth, Map<String, BigDecimal>> monthlyMap = new java.util.LinkedHashMap<>();
    for (YearMonth ym = startMonth; !ym.isAfter(endMonth); ym = ym.plusMonths(1)) {
      monthlyMap.put(ym, new java.util.LinkedHashMap<>());
    }
    for (Expense e : expenses) {
      YearMonth ym = YearMonth.from(e.getExpenseDate());
      Map<String, BigDecimal> monthMap = monthlyMap.get(ym);
      if (monthMap != null) {
        String cat = e.getCategory() != null ? e.getCategory().name() : "OTHER";
        monthMap.merge(cat, e.getAmount().value(), BigDecimal::add);
      }
    }

    // 2. Add actual financing payments to past months as FINANCING_PAYMENT category
    for (FinancingPayment fp : financingPayments) {
      YearMonth ym = YearMonth.from(fp.getPaymentDate());
      Map<String, BigDecimal> monthMap = monthlyMap.get(ym);
      if (monthMap != null && ym.isBefore(cutoff)) {
        monthMap.merge("FINANCING_PAYMENT", fp.getTotalAmount().value(), BigDecimal::add);
      }
    }

    // 3. Add operating costs from new tables to timeline (FUTURE MONTHS ONLY)
    Map<Integer, BigDecimal> opCosts = fd.operatingCostsByMonth();
    BigDecimal monthlyFinancing = fd.monthlyFinancingPayment().orElse(ZERO);
    for (Map.Entry<YearMonth, Map<String, BigDecimal>> entry : monthlyMap.entrySet()) {
      if (!entry.getKey().isBefore(cutoff)) {
        BigDecimal cost = opCosts.getOrDefault(entry.getKey().getMonthValue(), ZERO);
        if (cost.compareTo(ZERO) > 0) {
          entry.getValue().merge("OPERATING_COSTS", cost, BigDecimal::add);
        }
        if (monthlyFinancing.compareTo(ZERO) > 0) {
          entry.getValue().merge("FINANCING_PAYMENT", monthlyFinancing, BigDecimal::add);
        }
      }
    }

    // 3. Derive top-level category summary FROM the timeline (guarantees consistency)
    Map<String, BigDecimal> byCategory = new java.util.LinkedHashMap<>();
    for (Map<String, BigDecimal> monthData : monthlyMap.values()) {
      for (Map.Entry<String, BigDecimal> entry : monthData.entrySet()) {
        byCategory.merge(entry.getKey(), entry.getValue(), BigDecimal::add);
      }
    }

    List<CategorySlice> slices =
        byCategory.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .map(e -> new CategorySlice(e.getKey(), e.getValue()))
            .toList();

    List<ExpenseTimelineMonth> timeline =
        monthlyMap.entrySet().stream()
            .map(entry -> new ExpenseTimelineMonth(entry.getKey().toString(), entry.getValue()))
            .toList();

    return new ExpenseBreakdownChartData(slices, timeline);
  }

  private OccupancyChartData buildOccupancyChart(
      List<Contract> contracts,
      List<PropertyOccupancyPeriod> occupancyPeriods,
      LocalDate now,
      int months) {
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

      long tenantDays = 0;
      for (Contract c : occupiedContracts) {
        if (c.getStartDate() == null) {
          continue;
        }
        LocalDate cStart = c.getStartDate().isBefore(monthStart) ? monthStart : c.getStartDate();
        LocalDate cEnd = c.getEndDate().filter(d -> !d.isAfter(monthEnd)).orElse(monthEnd);
        if (!cStart.isAfter(cEnd)) {
          tenantDays += DAYS.between(cStart, cEnd) + 1;
        }
      }

      long selfDays = 0;
      for (PropertyOccupancyPeriod p : occupancyPeriods) {
        LocalDate pStart = p.getStartDate().isBefore(monthStart) ? monthStart : p.getStartDate();
        LocalDate pEnd = p.getEndDate().filter(d -> !d.isAfter(monthEnd)).orElse(monthEnd);
        if (!pStart.isAfter(pEnd)) {
          selfDays += DAYS.between(pStart, pEnd) + 1;
        }
      }

      // Cap individually at days in month
      tenantDays = Math.min(tenantDays, daysInMonth);
      selfDays = Math.min(selfDays, daysInMonth - tenantDays);

      BigDecimal tenantPct =
          BigDecimal.valueOf(tenantDays)
              .multiply(ONE_HUNDRED)
              .divide(BigDecimal.valueOf(daysInMonth), SCALE, HALF_UP);
      BigDecimal selfPct =
          BigDecimal.valueOf(selfDays)
              .multiply(ONE_HUNDRED)
              .divide(BigDecimal.valueOf(daysInMonth), SCALE, HALF_UP);
      dataPoints.add(new OccupancyDataPoint(ym.toString(), tenantPct, selfPct));
    }
    return new OccupancyChartData(dataPoints);
  }

  private FutureTrendData buildFutureTrend(
      FinancialData fd, List<Contract> contracts, LocalDate now) {
    List<FutureMonthDataPoint> points = new ArrayList<>();

    Map<Integer, BigDecimal> operatingCosts = fd.operatingCostsByMonth();
    BigDecimal monthlyFinancing = fd.monthlyFinancingPayment().orElse(ZERO);

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
                          && (c.getEndDate().isEmpty()
                              || !c.getEndDate().get().isBefore(monthStart)))
              .map(c -> c.getRentAmount() != null ? c.getRentAmount().value() : ZERO)
              .reduce(ZERO, BigDecimal::add);

      // Expected expenses: operating costs + financing payment
      BigDecimal expectedExpenses = operatingCosts.getOrDefault(monthNumber, ZERO);

      // Add financing payment
      if (monthlyFinancing.compareTo(ZERO) > 0) {
        expectedExpenses = expectedExpenses.add(monthlyFinancing);
      }

      BigDecimal expectedNet = expectedIncome.subtract(expectedExpenses);
      points.add(
          new FutureMonthDataPoint(
              futureMonth.toString(), expectedIncome, expectedExpenses, expectedNet));
    }

    return new FutureTrendData(points);
  }

  private DataCompleteness buildDataCompleteness(
      FinancialData fd, List<Contract> contracts, UUID propertyId, UUID teamId) {

    boolean hasPurchasePrice = fd.purchasePrice().isPresent();
    boolean hasMarketValue = fd.marketValue().isPresent();
    boolean hasMortgageInfo = fd.financingBalance().isPresent();
    boolean hasOperatingCosts = fd.annualOperatingCosts().compareTo(ZERO) > 0;
    boolean hasContracts = !contracts.isEmpty();
    // Use all-time checks for payments/expenses (independent of period filter)
    List<UUID> contractIds = contracts.stream().map(Contract::getId).toList();
    boolean hasPayments =
        !contractIds.isEmpty()
            && !paymentRepository
                .findPaidByContractIdsAndDateRange(
                    contractIds, teamId, LocalDate.of(1970, 1, 1), LocalDate.now(clock))
                .isEmpty();
    boolean hasExpenses = !expenseRepository.findByPropertyId(propertyId, teamId).isEmpty();

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
      LocalDate cEnd = c.getEndDate().filter(d -> !d.isAfter(now)).orElse(now);
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
