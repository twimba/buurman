package com.buurman.service;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.time.temporal.ChronoUnit.DAYS;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
  private static final int MONTHS_LOOKBACK = 12;
  public static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

  private final PropertyRepository propertyRepository;
  private final ContractRepository contractRepository;
  private final PaymentRepository paymentRepository;
  private final ExpenseRepository expenseRepository;

  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public PropertyDashboardResponse getDashboard(
      String propertyIdentifier, UserPrincipal principal) {
    return getDashboardData(propertyIdentifier, principal.getTeamId());
  }

  /** Internal method for use by other services (authorization handled by caller). */
  @Transactional(readOnly = true)
  public PropertyDashboardResponse getDashboardData(String propertyIdentifier, UUID teamId) {
    Property property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);

    List<Contract> contracts = contractRepository.findByPropertyId(property.getId(), teamId);
    List<UUID> contractIds = contracts.stream().map(Contract::getId).toList();

    String currency = property.getPurchasePriceCurrency();
    LocalDate now = LocalDate.now();
    LocalDate twelveMonthsAgo = now.minusMonths(MONTHS_LOOKBACK);

    // Fetch payments for all contracts of this property in the last 12 months (single query)
    List<Payment> allPayments =
        paymentRepository
            .findPaidByContractIdsAndDateRange(contractIds, teamId, twelveMonthsAgo, now)
            .stream()
            .filter(p -> currency == null || currency.equals(p.getCurrency()))
            .toList();

    List<Expense> allExpenses =
        expenseRepository.findByPropertyId(property.getId(), teamId).stream()
            .filter(
                e ->
                    e.getExpenseDate() != null
                        && !e.getExpenseDate().isBefore(twelveMonthsAgo)
                        && !e.getExpenseDate().isAfter(now))
            .filter(e -> currency == null || currency.equals(e.getCurrency()))
            .toList();
    SummaryMetrics summary = buildSummaryMetrics(property, contracts, allPayments, allExpenses);
    CashFlowChartData cashFlow = buildCashFlowChart(allPayments, allExpenses, property, now);
    EquityChartData equity = buildEquityChart(property);
    ExpenseBreakdownChartData expenseBreakdown = buildExpenseBreakdown(allExpenses, property, now);
    OccupancyChartData occupancy = buildOccupancyChart(contracts, now);
    DataCompleteness completeness =
        buildDataCompleteness(property, contracts, allPayments, allExpenses);

    return new PropertyDashboardResponse(
        summary, cashFlow, equity, expenseBreakdown, occupancy, completeness);
  }

  private SummaryMetrics buildSummaryMetrics(
      Property property, List<Contract> contracts, List<Payment> payments, List<Expense> expenses) {

    BigDecimal purchasePrice = property.getPurchasePrice();
    BigDecimal marketValue = property.getCurrentMarketValue();
    BigDecimal mortgageAmount = property.getMortgageAmount();
    BigDecimal monthlyMortgage = property.getMonthlyMortgagePayment();
    String currency = property.getPurchasePriceCurrency();

    BigDecimal totalIncome = sumAmounts(payments.stream().map(Payment::getAmount).toList());
    BigDecimal totalExpenses = sumAmounts(expenses.stream().map(Expense::getAmount).toList());

    // Annual projections from 12-month data
    BigDecimal annualIncome = totalIncome;
    BigDecimal annualExpenses = totalExpenses;
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

    // Monthly Cash Flow = avg monthly income - avg monthly expenses - mortgage payment
    BigDecimal avgMonthlyIncome = divideOrNull(annualIncome, BigDecimal.valueOf(MONTHS_LOOKBACK));
    BigDecimal avgMonthlyExpenses =
        divideOrNull(annualExpenses, BigDecimal.valueOf(MONTHS_LOOKBACK));
    BigDecimal monthlyCashFlow = null;
    if (avgMonthlyIncome != null) {
      monthlyCashFlow =
          avgMonthlyIncome
              .subtract(avgMonthlyExpenses != null ? avgMonthlyExpenses : ZERO)
              .subtract(monthlyMortgage != null ? monthlyMortgage : ZERO);
    }

    // Total Equity = market value - mortgage balance
    BigDecimal totalEquity = null;
    if (marketValue != null) {
      totalEquity = marketValue.subtract(mortgageAmount != null ? mortgageAmount : ZERO);
    }

    // Equity Growth % = (marketValue - purchasePrice) / purchasePrice * 100
    BigDecimal equityGrowthPercent = percentChange(purchasePrice, marketValue);

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

    // Occupancy Rate (last 12 months)
    BigDecimal occupancyRatePercent = calculateOccupancyRate(contracts, LocalDate.now());

    // Gross Rent Multiplier = market value / annual gross rent
    BigDecimal grossRentMultiplier = null;
    if (marketValue != null && annualIncome.compareTo(ZERO) > 0) {
      grossRentMultiplier = marketValue.divide(annualIncome, SCALE, HALF_UP);
    }

    return new SummaryMetrics(
        totalRoiPercent,
        annualizedRoiPercent,
        capRatePercent,
        cashOnCashPercent,
        monthlyCashFlow,
        annualNoi,
        totalEquity,
        equityGrowthPercent,
        occupancyRatePercent,
        grossRentMultiplier,
        currency);
  }

  private CashFlowChartData buildCashFlowChart(
      List<Payment> payments, List<Expense> expenses, Property property, LocalDate now) {
    BigDecimal monthlyMortgage = property.getMonthlyMortgagePayment();

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

    List<MonthlyDataPoint> months = new ArrayList<>();
    for (int i = MONTHS_LOOKBACK - 1; i >= 0; i--) {
      YearMonth ym = YearMonth.from(now.minusMonths(i));
      BigDecimal income = incomeByMonth.getOrDefault(ym, ZERO);
      BigDecimal exp = expensesByMonth.getOrDefault(ym, ZERO);
      BigDecimal mort = monthlyMortgage != null ? monthlyMortgage : ZERO;
      BigDecimal net = income.subtract(exp).subtract(mort);
      months.add(new MonthlyDataPoint(ym.toString(), income, exp, mort, net));
    }
    return new CashFlowChartData(months);
  }

  private EquityChartData buildEquityChart(Property property) {
    return new EquityChartData(
        property.getPurchasePrice(),
        property.getCurrentMarketValue(),
        property.getMortgageAmount());
  }

  private ExpenseBreakdownChartData buildExpenseBreakdown(
      List<Expense> expenses, Property property, LocalDate now) {
    Map<String, BigDecimal> byCategory =
        new java.util.LinkedHashMap<>(
            expenses.stream()
                .collect(
                    Collectors.groupingBy(
                        e -> e.getCategory() != null ? e.getCategory().name() : "OTHER",
                        Collectors.reducing(ZERO, Expense::getAmount, BigDecimal::add))));

    // Add property-level annual operating costs to breakdown
    addIfNotNull(byCategory, "PROPERTY_TAX", property.getAnnualPropertyTax());
    addIfNotNull(byCategory, "INSURANCE", property.getAnnualInsurance());
    addIfNotNull(byCategory, "HOA", property.getAnnualHoaFee());
    addIfNotNull(byCategory, "MANAGEMENT", property.getAnnualManagementFee());
    addIfNotNull(byCategory, "MAINTENANCE_RESERVE", property.getAnnualMaintenanceReserve());

    List<CategorySlice> slices =
        byCategory.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .map(e -> new CategorySlice(e.getKey(), e.getValue()))
            .toList();

    // Build monthly timeline grouped by category
    java.time.YearMonth startMonth = java.time.YearMonth.from(now.minusMonths(MONTHS_LOOKBACK - 1));
    java.time.YearMonth endMonth = java.time.YearMonth.from(now);

    // Group recorded expenses by month + category
    Map<java.time.YearMonth, Map<String, BigDecimal>> monthlyMap = new java.util.LinkedHashMap<>();
    for (java.time.YearMonth ym = startMonth; !ym.isAfter(endMonth); ym = ym.plusMonths(1)) {
      monthlyMap.put(ym, new java.util.LinkedHashMap<>());
    }
    for (Expense e : expenses) {
      java.time.YearMonth ym = java.time.YearMonth.from(e.getExpenseDate());
      Map<String, BigDecimal> monthMap = monthlyMap.get(ym);
      if (monthMap != null) {
        String cat = e.getCategory() != null ? e.getCategory().name() : "OTHER";
        monthMap.merge(cat, e.getAmount(), BigDecimal::add);
      }
    }

    // Distribute property-level annual costs evenly across months (1/12 each)
    Map<String, BigDecimal> monthlyCosts = new java.util.LinkedHashMap<>();
    addMonthlyShare(monthlyCosts, "PROPERTY_TAX", property.getAnnualPropertyTax());
    addMonthlyShare(monthlyCosts, "INSURANCE", property.getAnnualInsurance());
    addMonthlyShare(monthlyCosts, "HOA", property.getAnnualHoaFee());
    addMonthlyShare(monthlyCosts, "MANAGEMENT", property.getAnnualManagementFee());
    addMonthlyShare(monthlyCosts, "MAINTENANCE_RESERVE", property.getAnnualMaintenanceReserve());

    List<ExpenseTimelineMonth> timeline =
        monthlyMap.entrySet().stream()
            .map(
                entry -> {
                  Map<String, BigDecimal> combined =
                      new java.util.LinkedHashMap<>(entry.getValue());
                  monthlyCosts.forEach((k, v) -> combined.merge(k, v, BigDecimal::add));
                  return new ExpenseTimelineMonth(entry.getKey().toString(), combined);
                })
            .toList();

    return new ExpenseBreakdownChartData(slices, timeline);
  }

  private static void addMonthlyShare(
      Map<String, BigDecimal> map, String key, BigDecimal annualValue) {
    if (annualValue != null && annualValue.compareTo(ZERO) > 0) {
      map.put(
          key,
          annualValue.divide(
              BigDecimal.valueOf(MONTHS_LOOKBACK), SCALE, java.math.RoundingMode.HALF_UP));
    }
  }

  private static void addIfNotNull(Map<String, BigDecimal> map, String key, BigDecimal value) {
    if (value != null && value.compareTo(ZERO) > 0) {
      map.merge(key, value, BigDecimal::add);
    }
  }

  private OccupancyChartData buildOccupancyChart(List<Contract> contracts, LocalDate now) {
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

    List<OccupancyDataPoint> months = new ArrayList<>();
    for (int i = MONTHS_LOOKBACK - 1; i >= 0; i--) {
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
      months.add(new OccupancyDataPoint(ym.toString(), pct));
    }
    return new OccupancyChartData(months);
  }

  private DataCompleteness buildDataCompleteness(
      Property property, List<Contract> contracts, List<Payment> payments, List<Expense> expenses) {

    boolean hasPurchasePrice = property.getPurchasePrice() != null;
    boolean hasMarketValue = property.getCurrentMarketValue() != null;
    boolean hasMortgageInfo = property.getMortgageType() != null;
    boolean hasOperatingCosts =
        property.getAnnualPropertyTax() != null || property.getAnnualInsurance() != null;
    boolean hasContracts = !contracts.isEmpty();
    boolean hasPayments = !payments.isEmpty();
    boolean hasExpenses = !expenses.isEmpty();

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

  private BigDecimal calculateOccupancyRate(List<Contract> contracts, LocalDate now) {
    LocalDate start = now.minusMonths(MONTHS_LOOKBACK);
    long totalDays = DAYS.between(start, now);
    if (totalDays <= 0) {
      return null;
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

    return BigDecimal.valueOf(occupiedDays)
        .multiply(ONE_HUNDRED)
        .divide(BigDecimal.valueOf(totalDays), SCALE, HALF_UP);
  }

  private static BigDecimal sumAmounts(List<BigDecimal> amounts) {
    return amounts.stream().filter(java.util.Objects::nonNull).reduce(ZERO, BigDecimal::add);
  }

  private static BigDecimal divideOrNull(BigDecimal numerator, BigDecimal denominator) {
    if (numerator == null || denominator == null || denominator.compareTo(ZERO) == 0) {
      return null;
    }
    return numerator.divide(denominator, SCALE, HALF_UP);
  }

  private static BigDecimal percentChange(BigDecimal from, BigDecimal to) {
    if (from == null || to == null || from.compareTo(ZERO) == 0) {
      return null;
    }
    return to.subtract(from).multiply(ONE_HUNDRED).divide(from, SCALE, HALF_UP);
  }
}
