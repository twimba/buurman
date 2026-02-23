package com.buurman.service;

import static com.buurman.domain.Contract.ContractStatus.ACTIVE;
import static com.buurman.domain.Payment.PaymentStatus.PAID;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toMap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Contract;
import com.buurman.domain.Expense;
import com.buurman.domain.Payment;
import com.buurman.domain.Property;
import com.buurman.dto.response.CategoryExpenseSummary;
import com.buurman.dto.response.DataDateRangeResponse;
import com.buurman.dto.response.ExpenseBreakdownResponse;
import com.buurman.dto.response.FinancialOverviewResponse;
import com.buurman.dto.response.IncomeTrendResponse;
import com.buurman.dto.response.OccupancyTrendResponse;
import com.buurman.dto.response.PropertyComparisonResponse;
import com.buurman.dto.response.PropertyFinancialSummary;
import com.buurman.dto.response.TaxSummaryResponse;
import com.buurman.mapper.PropertyMapper;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.ExpenseRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {

  private final PaymentRepository paymentRepository;
  private final ExpenseRepository expenseRepository;
  private final PropertyRepository propertyRepository;
  private final ContractRepository contractRepository;
  private final PropertyMapper propertyMapper;
  private final TeamService teamService;

  // Color palette for charts
  private static final Map<String, String> EXPENSE_COLORS =
      Map.ofEntries(
          Map.entry("MAINTENANCE", "#3B82F6"),
          Map.entry("REPAIR", "#EF4444"),
          Map.entry("UTILITY", "#10B981"),
          Map.entry("TAX", "#F59E0B"),
          Map.entry("INSURANCE", "#8B5CF6"),
          Map.entry("LEGAL", "#EC4899"),
          Map.entry("MARKETING", "#14B8A6"),
          Map.entry("CLEANING", "#6366F1"),
          Map.entry("LANDSCAPING", "#84CC16"),
          Map.entry("PROPERTY_MANAGEMENT", "#06B6D4"),
          Map.entry("FEES", "#D97706"),
          Map.entry("PROPERTY_TAX", "#F43F5E"),
          Map.entry("OTHER", "#6B7280"));

  private final Clock clock;

  @Transactional(readOnly = true)
  public FinancialOverviewResponse getFinancialOverview(
      LocalDate startDate,
      LocalDate endDate,
      @Nullable List<UUID> propertyIds,
      @Nullable String currency,
      UserPrincipal principal) {

    UUID teamId = principal.requireTeamId();
    String activeCurrency =
        currency != null
            ? currency
            : teamService.getDefaultCurrency(principal.requireTeamId()).orElse("EUR");

    // Pre-fetch all contracts for the team to resolve payment→property mapping
    Map<UUID, Contract> contractsById =
        contractRepository.findAllByTeamId(teamId).stream()
            .collect(toMap(Contract::getId, identity()));

    // Get all payments in period (PAID status only)
    List<Payment> payments =
        paymentRepository.findByDateRange(startDate, endDate, teamId).stream()
            .filter(p -> p.getStatus() == PAID)
            .filter(
                p ->
                    propertyIds == null
                        || propertyIds.isEmpty()
                        || getPropertyIdFromContract(p.getContractId(), contractsById)
                            .filter(propertyIds::contains)
                            .isPresent())
            .toList();

    // Get all expenses in period
    List<Expense> expenses =
        expenseRepository.findByDateRange(startDate, endDate, teamId).stream()
            .filter(
                e ->
                    propertyIds == null
                        || propertyIds.isEmpty()
                        || propertyIds.contains(e.getPropertyId()))
            .toList();

    // Calculate total income
    BigDecimal totalIncome =
        payments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

    // Calculate income by property
    Map<UUID, BigDecimal> incomeByProperty =
        payments.stream()
            .filter(p -> getPropertyIdFromContract(p.getContractId(), contractsById).isPresent())
            .collect(
                groupingBy(
                    p -> getPropertyIdFromContract(p.getContractId(), contractsById).orElseThrow(),
                    reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)));

    // Calculate total expenses
    BigDecimal totalExpenses =
        expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

    // Calculate expenses by category
    List<CategoryExpenseSummary> expensesByCategory =
        calculateExpensesByCategory(expenses, totalExpenses);

    // Calculate expenses by property
    Map<UUID, BigDecimal> expensesByProperty =
        expenses.stream()
            .collect(
                groupingBy(
                    Expense::getPropertyId,
                    reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

    // Combine property data
    Set<UUID> allPropertyIds = new HashSet<>();
    allPropertyIds.addAll(incomeByProperty.keySet());
    allPropertyIds.addAll(expensesByProperty.keySet());
    allPropertyIds.remove(null);

    // Batch-fetch all properties
    Map<UUID, Property> propertiesById =
        propertyRepository.findByIdsAndTeamId(allPropertyIds, teamId).stream()
            .collect(toMap(Property::getId, identity()));

    // Pre-group contracts by property for occupancy calculation
    Map<UUID, List<Contract>> contractsByProperty =
        contractsById.values().stream()
            .filter(c -> c.getPropertyId() != null)
            .collect(groupingBy(Contract::getPropertyId));

    List<PropertyFinancialSummary> incomeByPropertyList =
        allPropertyIds.stream()
            .map(
                propId -> {
                  Property prop = propertiesById.get(propId);
                  if (prop == null) {
                    return null;
                  }
                  BigDecimal income = incomeByProperty.getOrDefault(propId, BigDecimal.ZERO);
                  return new PropertyFinancialSummary(
                      propertyMapper.toSummary(prop),
                      income,
                      BigDecimal.ZERO,
                      income,
                      calculateOccupancyDays(
                          contractsByProperty.getOrDefault(propId, List.of()), startDate, endDate));
                })
            .filter(Objects::nonNull)
            .toList();

    List<PropertyFinancialSummary> expensesByPropertyList =
        allPropertyIds.stream()
            .map(
                propId -> {
                  Property prop = propertiesById.get(propId);
                  if (prop == null) {
                    return null;
                  }
                  BigDecimal income = incomeByProperty.getOrDefault(propId, BigDecimal.ZERO);
                  BigDecimal expense = expensesByProperty.getOrDefault(propId, BigDecimal.ZERO);
                  BigDecimal netProfit = income.subtract(expense);
                  return new PropertyFinancialSummary(
                      propertyMapper.toSummary(prop),
                      income,
                      expense,
                      netProfit,
                      calculateOccupancyDays(
                          contractsByProperty.getOrDefault(propId, List.of()), startDate, endDate));
                })
            .filter(Objects::nonNull)
            .toList();

    // Calculate net profit
    BigDecimal netProfit = totalIncome.subtract(totalExpenses);

    return new FinancialOverviewResponse(
        new FinancialOverviewResponse.Period(startDate, endDate),
        new FinancialOverviewResponse.Income(totalIncome, incomeByPropertyList),
        new FinancialOverviewResponse.Expenses(
            totalExpenses, expensesByCategory, expensesByPropertyList),
        netProfit,
        activeCurrency);
  }

  @Transactional(readOnly = true)
  public IncomeTrendResponse getIncomeTrend(
      int months, @Nullable List<UUID> propertyIds, UserPrincipal principal) {

    UUID teamId = principal.requireTeamId();
    LocalDate endDate = LocalDate.now(clock);
    LocalDate startDate = endDate.minusMonths(months - 1).withDayOfMonth(1);
    LocalDate rangeEnd = YearMonth.from(endDate).atEndOfMonth();

    // Pre-fetch contracts for payment→property mapping (needed for property filtering)
    Map<UUID, Contract> contractsById =
        contractRepository.findAllByTeamId(teamId).stream()
            .collect(toMap(Contract::getId, identity()));

    // Fetch full range once
    List<Payment> allPayments =
        paymentRepository.findByDateRange(startDate, rangeEnd, teamId).stream()
            .filter(p -> p.getStatus() == PAID)
            .filter(p -> matchesPropertyFilter(p, propertyIds, contractsById))
            .toList();
    List<Expense> allExpenses =
        expenseRepository.findByDateRange(startDate, rangeEnd, teamId).stream()
            .filter(e -> matchesPropertyFilter(e, propertyIds))
            .toList();

    // Group by month
    Map<YearMonth, BigDecimal> incomeByMonth =
        allPayments.stream()
            .collect(
                groupingBy(
                    p ->
                        YearMonth.from(
                            p.getPaymentDate() != null ? p.getPaymentDate() : p.getDueDate()),
                    reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)));
    Map<YearMonth, BigDecimal> expensesByMonth =
        allExpenses.stream()
            .collect(
                groupingBy(
                    e -> YearMonth.from(e.getExpenseDate()),
                    reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

    List<IncomeTrendResponse.DataPoint> dataPoints = new ArrayList<>();

    for (int i = 0; i < months; i++) {
      YearMonth month = YearMonth.from(startDate.plusMonths(i));
      BigDecimal monthIncome = incomeByMonth.getOrDefault(month, BigDecimal.ZERO);
      BigDecimal monthExpenses = expensesByMonth.getOrDefault(month, BigDecimal.ZERO);
      BigDecimal monthNetProfit = monthIncome.subtract(monthExpenses);

      dataPoints.add(
          new IncomeTrendResponse.DataPoint(
              month.toString(), monthIncome, monthExpenses, monthNetProfit));
    }

    return new IncomeTrendResponse(
        dataPoints, teamService.getDefaultCurrency(principal.requireTeamId()).orElse("EUR"));
  }

  @Transactional(readOnly = true)
  public IncomeTrendResponse getIncomeTrendByDateRange(
      LocalDate startDate,
      LocalDate endDate,
      @Nullable List<UUID> propertyIds,
      UserPrincipal principal) {

    UUID teamId = principal.requireTeamId();
    LocalDate rangeStart = startDate.withDayOfMonth(1);
    LocalDate rangeEnd = YearMonth.from(endDate).atEndOfMonth();

    Map<UUID, Contract> contractsById =
        contractRepository.findAllByTeamId(teamId).stream()
            .collect(toMap(Contract::getId, identity()));

    List<Payment> allPayments =
        paymentRepository.findByDateRange(rangeStart, rangeEnd, teamId).stream()
            .filter(p -> p.getStatus() == PAID)
            .filter(p -> matchesPropertyFilter(p, propertyIds, contractsById))
            .toList();
    List<Expense> allExpenses =
        expenseRepository.findByDateRange(rangeStart, rangeEnd, teamId).stream()
            .filter(e -> matchesPropertyFilter(e, propertyIds))
            .toList();

    Map<YearMonth, BigDecimal> incomeByMonth =
        allPayments.stream()
            .collect(
                groupingBy(
                    p ->
                        YearMonth.from(
                            p.getPaymentDate() != null ? p.getPaymentDate() : p.getDueDate()),
                    reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)));
    Map<YearMonth, BigDecimal> expensesByMonth =
        allExpenses.stream()
            .collect(
                groupingBy(
                    e -> YearMonth.from(e.getExpenseDate()),
                    reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

    List<IncomeTrendResponse.DataPoint> dataPoints = new ArrayList<>();
    YearMonth start = YearMonth.from(rangeStart);
    YearMonth end = YearMonth.from(endDate);

    for (YearMonth month = start; !month.isAfter(end); month = month.plusMonths(1)) {
      BigDecimal monthIncome = incomeByMonth.getOrDefault(month, BigDecimal.ZERO);
      BigDecimal monthExpenses = expensesByMonth.getOrDefault(month, BigDecimal.ZERO);
      BigDecimal monthNetProfit = monthIncome.subtract(monthExpenses);

      dataPoints.add(
          new IncomeTrendResponse.DataPoint(
              month.toString(), monthIncome, monthExpenses, monthNetProfit));
    }

    return new IncomeTrendResponse(
        dataPoints, teamService.getDefaultCurrency(principal.requireTeamId()).orElse("EUR"));
  }

  @Transactional(readOnly = true)
  public ExpenseBreakdownResponse getExpenseBreakdown(
      LocalDate startDate,
      LocalDate endDate,
      @Nullable List<UUID> propertyIds,
      UserPrincipal principal) {

    UUID teamId = principal.requireTeamId();

    List<Expense> expenses =
        expenseRepository.findByDateRange(startDate, endDate, teamId).stream()
            .filter(e -> matchesPropertyFilter(e, propertyIds))
            .toList();

    BigDecimal total =
        expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

    Map<Expense.ExpenseCategory, BigDecimal> expensesByCategory =
        expenses.stream()
            .collect(
                groupingBy(
                    Expense::getCategory,
                    reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

    List<ExpenseBreakdownResponse.Category> categories =
        expensesByCategory.entrySet().stream()
            .map(
                entry ->
                    new ExpenseBreakdownResponse.Category(
                        entry.getKey().name(),
                        entry.getValue(),
                        EXPENSE_COLORS.getOrDefault(entry.getKey().name(), "#6B7280")))
            .sorted(Comparator.comparing(ExpenseBreakdownResponse.Category::value).reversed())
            .toList();

    return new ExpenseBreakdownResponse(
        categories, total, teamService.getDefaultCurrency(principal.requireTeamId()).orElse("EUR"));
  }

  @Transactional(readOnly = true)
  public PropertyComparisonResponse getPropertyComparison(
      LocalDate startDate,
      LocalDate endDate,
      @Nullable List<UUID> propertyIds,
      UserPrincipal principal) {

    UUID teamId = principal.requireTeamId();

    // Pre-fetch all contracts for payment→property mapping
    Map<UUID, Contract> contractsById =
        contractRepository.findAllByTeamId(teamId).stream()
            .collect(toMap(Contract::getId, identity()));

    // Get all payments and expenses in period
    List<Payment> payments =
        paymentRepository.findByDateRange(startDate, endDate, teamId).stream()
            .filter(p -> p.getStatus() == PAID)
            .filter(p -> matchesPropertyFilter(p, propertyIds, contractsById))
            .toList();

    List<Expense> expenses =
        expenseRepository.findByDateRange(startDate, endDate, teamId).stream()
            .filter(e -> matchesPropertyFilter(e, propertyIds))
            .toList();

    // Group by property
    Map<UUID, BigDecimal> incomeByProperty =
        payments.stream()
            .filter(p -> getPropertyIdFromContract(p.getContractId(), contractsById).isPresent())
            .collect(
                groupingBy(
                    p -> getPropertyIdFromContract(p.getContractId(), contractsById).orElseThrow(),
                    reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)));

    Map<UUID, BigDecimal> expensesByProperty =
        expenses.stream()
            .collect(
                groupingBy(
                    Expense::getPropertyId,
                    reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

    Set<UUID> allPropertyIds = new HashSet<>();
    allPropertyIds.addAll(incomeByProperty.keySet());
    allPropertyIds.addAll(expensesByProperty.keySet());
    allPropertyIds.remove(null);

    // Batch-fetch all properties
    Map<UUID, Property> propertiesById =
        propertyRepository.findByIdsAndTeamId(allPropertyIds, teamId).stream()
            .collect(toMap(Property::getId, identity()));

    List<PropertyComparisonResponse.PropertyData> propertyData =
        allPropertyIds.stream()
            .map(
                propId -> {
                  Property prop = propertiesById.get(propId);
                  if (prop == null) {
                    return null;
                  }
                  BigDecimal income = incomeByProperty.getOrDefault(propId, BigDecimal.ZERO);
                  BigDecimal expense = expensesByProperty.getOrDefault(propId, BigDecimal.ZERO);
                  BigDecimal netProfit = income.subtract(expense);
                  return new PropertyComparisonResponse.PropertyData(
                      propertyMapper.toSummary(prop), income, expense, netProfit);
                })
            .filter(Objects::nonNull)
            .sorted(
                Comparator.comparing(PropertyComparisonResponse.PropertyData::netProfit).reversed())
            .toList();

    return new PropertyComparisonResponse(
        propertyData, teamService.getDefaultCurrency(principal.requireTeamId()).orElse("EUR"));
  }

  @Transactional(readOnly = true)
  public OccupancyTrendResponse getOccupancyTrend(int months, UserPrincipal principal) {

    UUID teamId = principal.requireTeamId();
    LocalDate endDate = LocalDate.now(clock);
    LocalDate startDate = endDate.minusMonths(months - 1).withDayOfMonth(1);

    int totalProperties = propertyRepository.findAllByTeamId(teamId).size();

    // Fetch all active contracts once, outside the loop
    List<Contract> allActiveContracts =
        contractRepository.findAllByTeamId(teamId).stream()
            .filter(c -> c.getStatus() == ACTIVE)
            .toList();

    List<OccupancyTrendResponse.DataPoint> dataPoints = new ArrayList<>();

    for (int i = 0; i < months; i++) {
      YearMonth month = YearMonth.from(startDate.plusMonths(i));
      LocalDate monthStart = month.atDay(1);
      LocalDate monthEnd = month.atEndOfMonth();

      // Filter pre-fetched contracts for this month
      long occupiedUnits =
          allActiveContracts.stream()
              .filter(
                  c -> {
                    LocalDate contractStart = c.getStartDate();
                    LocalDate contractEnd = c.getEndDate().orElse(LocalDate.MAX);
                    return !contractStart.isAfter(monthEnd) && !contractEnd.isBefore(monthStart);
                  })
              .count();

      double occupancyRate = totalProperties > 0 ? (occupiedUnits * 100.0) / totalProperties : 0.0;

      dataPoints.add(
          new OccupancyTrendResponse.DataPoint(
              month.toString(),
              Math.round(occupancyRate * 100.0) / 100.0,
              totalProperties,
              (int) occupiedUnits));
    }

    return new OccupancyTrendResponse(dataPoints);
  }

  @Transactional(readOnly = true)
  public OccupancyTrendResponse getOccupancyTrendByDateRange(
      LocalDate startDate, LocalDate endDate, UserPrincipal principal) {

    UUID teamId = principal.requireTeamId();
    int totalProperties = propertyRepository.findAllByTeamId(teamId).size();

    List<Contract> allActiveContracts =
        contractRepository.findAllByTeamId(teamId).stream()
            .filter(c -> c.getStatus() == ACTIVE)
            .toList();

    List<OccupancyTrendResponse.DataPoint> dataPoints = new ArrayList<>();
    YearMonth start = YearMonth.from(startDate.withDayOfMonth(1));
    YearMonth end = YearMonth.from(endDate);

    for (YearMonth month = start; !month.isAfter(end); month = month.plusMonths(1)) {
      LocalDate monthStart = month.atDay(1);
      LocalDate monthEnd = month.atEndOfMonth();

      long occupiedUnits =
          allActiveContracts.stream()
              .filter(
                  c -> {
                    LocalDate contractStart = c.getStartDate();
                    LocalDate contractEnd = c.getEndDate().orElse(LocalDate.MAX);
                    return !contractStart.isAfter(monthEnd) && !contractEnd.isBefore(monthStart);
                  })
              .count();

      double occupancyRate = totalProperties > 0 ? (occupiedUnits * 100.0) / totalProperties : 0.0;

      dataPoints.add(
          new OccupancyTrendResponse.DataPoint(
              month.toString(),
              Math.round(occupancyRate * 100.0) / 100.0,
              totalProperties,
              (int) occupiedUnits));
    }

    return new OccupancyTrendResponse(dataPoints);
  }

  @Transactional(readOnly = true)
  public TaxSummaryResponse getTaxSummary(int year, UserPrincipal principal) {

    UUID teamId = principal.requireTeamId();
    LocalDate startDate = LocalDate.of(year, 1, 1);
    LocalDate endDate = LocalDate.of(year, 12, 31);

    // Pre-fetch all contracts for payment→property mapping and occupancy
    Map<UUID, Contract> contractsById =
        contractRepository.findAllByTeamId(teamId).stream()
            .collect(toMap(Contract::getId, identity()));

    // Get all paid payments for the year
    List<Payment> payments =
        paymentRepository.findByDateRange(startDate, endDate, teamId).stream()
            .filter(p -> p.getStatus() == PAID)
            .toList();

    BigDecimal totalIncome =
        payments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

    // Get all expenses for the year
    List<Expense> expenses = expenseRepository.findByDateRange(startDate, endDate, teamId);

    BigDecimal totalExpenses =
        expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal netIncome = totalIncome.subtract(totalExpenses);

    // Calculate expenses by category
    List<CategoryExpenseSummary> expensesByCategory =
        calculateExpensesByCategory(expenses, totalExpenses);

    // Calculate by property
    Map<UUID, BigDecimal> incomeByProperty =
        payments.stream()
            .filter(p -> getPropertyIdFromContract(p.getContractId(), contractsById).isPresent())
            .collect(
                groupingBy(
                    p -> getPropertyIdFromContract(p.getContractId(), contractsById).orElseThrow(),
                    reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)));

    Map<UUID, BigDecimal> expensesByProperty =
        expenses.stream()
            .collect(
                groupingBy(
                    Expense::getPropertyId,
                    reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

    Set<UUID> allPropertyIds = new HashSet<>();
    allPropertyIds.addAll(incomeByProperty.keySet());
    allPropertyIds.addAll(expensesByProperty.keySet());
    allPropertyIds.remove(null);

    // Batch-fetch all properties
    Map<UUID, Property> propertiesById =
        propertyRepository.findByIdsAndTeamId(allPropertyIds, teamId).stream()
            .collect(toMap(Property::getId, identity()));

    // Pre-group contracts by property for occupancy
    Map<UUID, List<Contract>> contractsByProperty =
        contractsById.values().stream()
            .filter(c -> c.getPropertyId() != null)
            .collect(groupingBy(Contract::getPropertyId));

    List<PropertyFinancialSummary> properties =
        allPropertyIds.stream()
            .map(
                propId -> {
                  Property prop = propertiesById.get(propId);
                  if (prop == null) {
                    return null;
                  }
                  BigDecimal income = incomeByProperty.getOrDefault(propId, BigDecimal.ZERO);
                  BigDecimal expense = expensesByProperty.getOrDefault(propId, BigDecimal.ZERO);
                  BigDecimal netProfit = income.subtract(expense);
                  return new PropertyFinancialSummary(
                      propertyMapper.toSummary(prop),
                      income,
                      expense,
                      netProfit,
                      calculateOccupancyDays(
                          contractsByProperty.getOrDefault(propId, List.of()), startDate, endDate));
                })
            .filter(Objects::nonNull)
            .toList();

    return new TaxSummaryResponse(
        year,
        totalIncome,
        totalExpenses,
        netIncome,
        expensesByCategory,
        properties,
        teamService.getDefaultCurrency(principal.requireTeamId()).orElse("EUR"));
  }

  @Transactional(readOnly = true)
  public DataDateRangeResponse getDataDateRange(UserPrincipal principal) {
    UUID teamId = principal.requireTeamId();

    @Nullable LocalDate earliest = null;

    // Earliest payment date
    @Nullable LocalDate earliestPayment = paymentRepository.findEarliestPaymentDate(teamId);
    if (earliestPayment != null && (earliest == null || earliestPayment.isBefore(earliest))) {
      earliest = earliestPayment;
    }

    // Earliest expense date
    @Nullable LocalDate earliestExpense = expenseRepository.findEarliestExpenseDate(teamId);
    if (earliestExpense != null && (earliest == null || earliestExpense.isBefore(earliest))) {
      earliest = earliestExpense;
    }

    // Earliest contract start date
    @Nullable LocalDate earliestContract = contractRepository.findEarliestStartDate(teamId);
    if (earliestContract != null && (earliest == null || earliestContract.isBefore(earliest))) {
      earliest = earliestContract;
    }

    return new DataDateRangeResponse(earliest);
  }

  // Helper methods

  private Optional<UUID> getPropertyIdFromContract(
      UUID contractId, Map<UUID, Contract> contractsById) {
    return Optional.ofNullable(contractsById.get(contractId)).map(Contract::getPropertyId);
  }

  private List<CategoryExpenseSummary> calculateExpensesByCategory(
      List<Expense> expenses, BigDecimal total) {

    Map<Expense.ExpenseCategory, BigDecimal> expensesByCategory =
        expenses.stream()
            .collect(
                groupingBy(
                    Expense::getCategory,
                    reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

    Map<Expense.ExpenseCategory, Long> countsByCategory =
        expenses.stream().collect(groupingBy(Expense::getCategory, counting()));

    return expensesByCategory.entrySet().stream()
        .map(
            entry -> {
              double percentage =
                  total.compareTo(BigDecimal.ZERO) > 0
                      ? entry
                          .getValue()
                          .divide(total, 4, RoundingMode.HALF_UP)
                          .multiply(BigDecimal.valueOf(100))
                          .doubleValue()
                      : 0.0;

              return new CategoryExpenseSummary(
                  entry.getKey().name(),
                  entry.getValue(),
                  countsByCategory.getOrDefault(entry.getKey(), 0L).intValue(),
                  Math.round(percentage * 100.0) / 100.0);
            })
        .sorted(Comparator.comparing(CategoryExpenseSummary::total).reversed())
        .toList();
  }

  private boolean matchesPropertyFilter(
      Payment payment, @Nullable List<UUID> propertyIds, Map<UUID, Contract> contractsById) {
    if (propertyIds == null || propertyIds.isEmpty()) {
      return true;
    }
    return getPropertyIdFromContract(payment.getContractId(), contractsById)
        .map(propertyIds::contains)
        .orElse(false);
  }

  private boolean matchesPropertyFilter(Expense expense, @Nullable List<UUID> propertyIds) {
    if (propertyIds == null || propertyIds.isEmpty()) {
      return true;
    }
    return propertyIds.contains(expense.getPropertyId());
  }

  private int calculateOccupancyDays(
      List<Contract> contracts, LocalDate startDate, LocalDate endDate) {
    long totalDays = 0;
    for (Contract contract : contracts) {
      if (contract.getStatus() != ACTIVE) {
        continue;
      }

      LocalDate contractStart = contract.getStartDate();
      LocalDate contractEnd = contract.getEndDate().orElse(LocalDate.MAX);

      // Calculate overlap
      LocalDate overlapStart = contractStart.isBefore(startDate) ? startDate : contractStart;
      LocalDate overlapEnd = contractEnd.isAfter(endDate) ? endDate : contractEnd;

      if (!overlapStart.isAfter(overlapEnd)) {
        totalDays += DAYS.between(overlapStart, overlapEnd) + 1;
      }
    }

    return (int) totalDays;
  }
}
