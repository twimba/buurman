package com.buurman.service;

import com.buurman.domain.Contract;
import com.buurman.domain.Expense;
import com.buurman.domain.Payment;
import com.buurman.dto.response.*;
import com.buurman.mapper.PropertyMapper;
import com.buurman.repository.ContractRepository;
import com.buurman.repository.ExpenseRepository;
import com.buurman.repository.PaymentRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.Property;

import static com.buurman.domain.Contract.ContractStatus.ACTIVE;
import static com.buurman.domain.Payment.PaymentStatus.PAID;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;
    private final PropertyRepository propertyRepository;
    private final ContractRepository contractRepository;
    private final PropertyMapper propertyMapper;

    // Color palette for charts
    private static final Map<String, String> EXPENSE_COLORS = Map.ofEntries(
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
            Map.entry("OTHER", "#6B7280")
    );

    private final Clock clock;

    public ReportService(PaymentRepository paymentRepository,
                        ExpenseRepository expenseRepository,
                        PropertyRepository propertyRepository,
                        ContractRepository contractRepository,
                        PropertyMapper propertyMapper,
                        Clock clock) {
        this.paymentRepository = paymentRepository;
        this.expenseRepository = expenseRepository;
        this.propertyRepository = propertyRepository;
        this.contractRepository = contractRepository;
        this.propertyMapper = propertyMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public FinancialOverviewResponse getFinancialOverview(
            LocalDate startDate,
            LocalDate endDate,
            List<UUID> propertyIds,
            String currency,
            UserPrincipal principal) {

        UUID teamId = principal.getTeamId();
        String activeCurrency = currency != null ? currency : "EUR";

        // Pre-fetch all contracts for the team to resolve payment→property mapping
        Map<UUID, Contract> contractsById = contractRepository.findAllByTeamId(teamId).stream()
                .collect(toMap(Contract::getId, identity()));

        // Get all payments in period (PAID status only)
        List<Payment> payments = paymentRepository.findByDateRange(startDate, endDate, teamId).stream()
                .filter(p -> p.getStatus() == PAID)
                .filter(p -> propertyIds == null || propertyIds.isEmpty() ||
                        propertyIds.contains(getPropertyIdFromContract(p.getContractId(), contractsById)))
                .toList();

        // Get all expenses in period
        List<Expense> expenses = expenseRepository.findByDateRange(startDate, endDate, teamId).stream()
                .filter(e -> propertyIds == null || propertyIds.isEmpty() || propertyIds.contains(e.getPropertyId()))
                .toList();

        // Calculate total income
        BigDecimal totalIncome = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate income by property
        Map<UUID, BigDecimal> incomeByProperty = payments.stream()
                .collect(groupingBy(
                        p -> getPropertyIdFromContract(p.getContractId(), contractsById),
                        reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)
                ));

        // Calculate total expenses
        BigDecimal totalExpenses = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate expenses by category
        List<CategoryExpenseSummary> expensesByCategory = calculateExpensesByCategory(expenses, totalExpenses);

        // Calculate expenses by property
        Map<UUID, BigDecimal> expensesByProperty = expenses.stream()
                .collect(groupingBy(
                        Expense::getPropertyId,
                        reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        // Combine property data
        Set<UUID> allPropertyIds = new HashSet<>();
        allPropertyIds.addAll(incomeByProperty.keySet());
        allPropertyIds.addAll(expensesByProperty.keySet());
        allPropertyIds.remove(null);

        // Batch-fetch all properties
        Map<UUID, Property> propertiesById = propertyRepository.findByIdsAndTeamId(allPropertyIds, teamId).stream()
                .collect(toMap(Property::getId, identity()));

        // Pre-group contracts by property for occupancy calculation
        Map<UUID, List<Contract>> contractsByProperty = contractsById.values().stream()
                .filter(c -> c.getPropertyId() != null)
                .collect(groupingBy(Contract::getPropertyId));

        List<PropertyFinancialSummary> incomeByPropertyList = allPropertyIds.stream()
                .map(propId -> {
                    Property prop = propertiesById.get(propId);
                    if (prop == null) return null;
                    BigDecimal income = incomeByProperty.getOrDefault(propId, BigDecimal.ZERO);
                    return new PropertyFinancialSummary(
                            propertyMapper.toSummary(prop),
                            income,
                            BigDecimal.ZERO,
                            income,
                            calculateOccupancyDays(contractsByProperty.getOrDefault(propId, List.of()), startDate, endDate)
                    );
                })
                .filter(Objects::nonNull)
                .toList();

        List<PropertyFinancialSummary> expensesByPropertyList = allPropertyIds.stream()
                .map(propId -> {
                    Property prop = propertiesById.get(propId);
                    if (prop == null) return null;
                    BigDecimal income = incomeByProperty.getOrDefault(propId, BigDecimal.ZERO);
                    BigDecimal expense = expensesByProperty.getOrDefault(propId, BigDecimal.ZERO);
                    BigDecimal netProfit = income.subtract(expense);
                    return new PropertyFinancialSummary(
                            propertyMapper.toSummary(prop),
                            income,
                            expense,
                            netProfit,
                            calculateOccupancyDays(contractsByProperty.getOrDefault(propId, List.of()), startDate, endDate)
                    );
                })
                .filter(Objects::nonNull)
                .toList();

        // Calculate net profit
        BigDecimal netProfit = totalIncome.subtract(totalExpenses);

        return new FinancialOverviewResponse(
                new FinancialOverviewResponse.Period(startDate, endDate),
                new FinancialOverviewResponse.Income(totalIncome, incomeByPropertyList),
                new FinancialOverviewResponse.Expenses(totalExpenses, expensesByCategory, expensesByPropertyList),
                netProfit,
                activeCurrency
        );
    }

    @Transactional(readOnly = true)
    public IncomeTrendResponse getIncomeTrend(
            int months,
            UserPrincipal principal) {

        UUID teamId = principal.getTeamId();
        LocalDate endDate = LocalDate.now(clock);
        LocalDate startDate = endDate.minusMonths(months - 1).withDayOfMonth(1);
        LocalDate rangeEnd = YearMonth.from(endDate).atEndOfMonth();

        // Fetch full range once
        List<Payment> allPayments = paymentRepository.findByDateRange(startDate, rangeEnd, teamId).stream()
                .filter(p -> p.getStatus() == PAID)
                .toList();
        List<Expense> allExpenses = expenseRepository.findByDateRange(startDate, rangeEnd, teamId);

        // Group by month
        Map<YearMonth, BigDecimal> incomeByMonth = allPayments.stream()
                .collect(groupingBy(
                        p -> YearMonth.from(p.getPaymentDate() != null ? p.getPaymentDate() : p.getDueDate()),
                        reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)
                ));
        Map<YearMonth, BigDecimal> expensesByMonth = allExpenses.stream()
                .collect(groupingBy(
                        e -> YearMonth.from(e.getExpenseDate()),
                        reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        List<IncomeTrendResponse.DataPoint> dataPoints = new ArrayList<>();

        for (int i = 0; i < months; i++) {
            YearMonth month = YearMonth.from(startDate.plusMonths(i));
            BigDecimal monthIncome = incomeByMonth.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal monthExpenses = expensesByMonth.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal monthNetProfit = monthIncome.subtract(monthExpenses);

            dataPoints.add(new IncomeTrendResponse.DataPoint(
                    month.toString(),
                    monthIncome,
                    monthExpenses,
                    monthNetProfit
            ));
        }

        return new IncomeTrendResponse(dataPoints, "EUR");
    }

    @Transactional(readOnly = true)
    public ExpenseBreakdownResponse getExpenseBreakdown(
            LocalDate startDate,
            LocalDate endDate,
            UserPrincipal principal) {

        UUID teamId = principal.getTeamId();

        List<Expense> expenses = expenseRepository.findByDateRange(startDate, endDate, teamId);

        BigDecimal total = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Expense.ExpenseCategory, BigDecimal> expensesByCategory = expenses.stream()
                .collect(groupingBy(
                        Expense::getCategory,
                        reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        List<ExpenseBreakdownResponse.Category> categories = expensesByCategory.entrySet().stream()
                .map(entry -> new ExpenseBreakdownResponse.Category(
                        entry.getKey().name(),
                        entry.getValue(),
                        EXPENSE_COLORS.getOrDefault(entry.getKey().name(), "#6B7280")
                ))
                .sorted(Comparator.comparing(ExpenseBreakdownResponse.Category::value).reversed())
                .toList();

        return new ExpenseBreakdownResponse(categories, total, "EUR");
    }

    @Transactional(readOnly = true)
    public PropertyComparisonResponse getPropertyComparison(
            LocalDate startDate,
            LocalDate endDate,
            UserPrincipal principal) {

        UUID teamId = principal.getTeamId();

        // Pre-fetch all contracts for payment→property mapping
        Map<UUID, Contract> contractsById = contractRepository.findAllByTeamId(teamId).stream()
                .collect(toMap(Contract::getId, identity()));

        // Get all payments and expenses in period
        List<Payment> payments = paymentRepository.findByDateRange(startDate, endDate, teamId).stream()
                .filter(p -> p.getStatus() == PAID)
                .toList();

        List<Expense> expenses = expenseRepository.findByDateRange(startDate, endDate, teamId);

        // Group by property
        Map<UUID, BigDecimal> incomeByProperty = payments.stream()
                .collect(groupingBy(
                        p -> getPropertyIdFromContract(p.getContractId(), contractsById),
                        reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)
                ));

        Map<UUID, BigDecimal> expensesByProperty = expenses.stream()
                .collect(groupingBy(
                        Expense::getPropertyId,
                        reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        Set<UUID> allPropertyIds = new HashSet<>();
        allPropertyIds.addAll(incomeByProperty.keySet());
        allPropertyIds.addAll(expensesByProperty.keySet());
        allPropertyIds.remove(null);

        // Batch-fetch all properties
        Map<UUID, Property> propertiesById = propertyRepository.findByIdsAndTeamId(allPropertyIds, teamId).stream()
                .collect(toMap(Property::getId, identity()));

        List<PropertyComparisonResponse.PropertyData> propertyData = allPropertyIds.stream()
                .map(propId -> {
                    Property prop = propertiesById.get(propId);
                    if (prop == null) return null;
                    BigDecimal income = incomeByProperty.getOrDefault(propId, BigDecimal.ZERO);
                    BigDecimal expense = expensesByProperty.getOrDefault(propId, BigDecimal.ZERO);
                    BigDecimal netProfit = income.subtract(expense);
                    return new PropertyComparisonResponse.PropertyData(
                            propertyMapper.toSummary(prop),
                            income,
                            expense,
                            netProfit
                    );
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(PropertyComparisonResponse.PropertyData::netProfit).reversed())
                .toList();

        return new PropertyComparisonResponse(propertyData, "EUR");
    }

    @Transactional(readOnly = true)
    public OccupancyTrendResponse getOccupancyTrend(
            int months,
            UserPrincipal principal) {

        UUID teamId = principal.getTeamId();
        LocalDate endDate = LocalDate.now(clock);
        LocalDate startDate = endDate.minusMonths(months - 1).withDayOfMonth(1);

        int totalProperties = propertyRepository.findAllByTeamId(teamId).size();

        // Fetch all active contracts once, outside the loop
        List<Contract> allActiveContracts = contractRepository.findAllByTeamId(teamId).stream()
                .filter(c -> c.getStatus() == ACTIVE)
                .toList();

        List<OccupancyTrendResponse.DataPoint> dataPoints = new ArrayList<>();

        for (int i = 0; i < months; i++) {
            YearMonth month = YearMonth.from(startDate.plusMonths(i));
            LocalDate monthStart = month.atDay(1);
            LocalDate monthEnd = month.atEndOfMonth();

            // Filter pre-fetched contracts for this month
            long occupiedUnits = allActiveContracts.stream()
                    .filter(c -> {
                        LocalDate contractStart = c.getStartDate();
                        LocalDate contractEnd = c.getEndDate() != null ? c.getEndDate() : LocalDate.MAX;
                        return !contractStart.isAfter(monthEnd) && !contractEnd.isBefore(monthStart);
                    })
                    .count();

            double occupancyRate = totalProperties > 0
                    ? (occupiedUnits * 100.0) / totalProperties
                    : 0.0;

            dataPoints.add(new OccupancyTrendResponse.DataPoint(
                    month.toString(),
                    Math.round(occupancyRate * 100.0) / 100.0,
                    totalProperties,
                    (int) occupiedUnits
            ));
        }

        return new OccupancyTrendResponse(dataPoints);
    }

    @Transactional(readOnly = true)
    public TaxSummaryResponse getTaxSummary(
            int year,
            UserPrincipal principal) {

        UUID teamId = principal.getTeamId();
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        // Pre-fetch all contracts for payment→property mapping and occupancy
        Map<UUID, Contract> contractsById = contractRepository.findAllByTeamId(teamId).stream()
                .collect(toMap(Contract::getId, identity()));

        // Get all paid payments for the year
        List<Payment> payments = paymentRepository.findByDateRange(startDate, endDate, teamId).stream()
                .filter(p -> p.getStatus() == PAID)
                .toList();

        BigDecimal totalIncome = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get all expenses for the year
        List<Expense> expenses = expenseRepository.findByDateRange(startDate, endDate, teamId);

        BigDecimal totalExpenses = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netIncome = totalIncome.subtract(totalExpenses);

        // Calculate expenses by category
        List<CategoryExpenseSummary> expensesByCategory = calculateExpensesByCategory(expenses, totalExpenses);

        // Calculate by property
        Map<UUID, BigDecimal> incomeByProperty = payments.stream()
                .collect(groupingBy(
                        p -> getPropertyIdFromContract(p.getContractId(), contractsById),
                        reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)
                ));

        Map<UUID, BigDecimal> expensesByProperty = expenses.stream()
                .collect(groupingBy(
                        Expense::getPropertyId,
                        reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        Set<UUID> allPropertyIds = new HashSet<>();
        allPropertyIds.addAll(incomeByProperty.keySet());
        allPropertyIds.addAll(expensesByProperty.keySet());
        allPropertyIds.remove(null);

        // Batch-fetch all properties
        Map<UUID, Property> propertiesById = propertyRepository.findByIdsAndTeamId(allPropertyIds, teamId).stream()
                .collect(toMap(Property::getId, identity()));

        // Pre-group contracts by property for occupancy
        Map<UUID, List<Contract>> contractsByProperty = contractsById.values().stream()
                .filter(c -> c.getPropertyId() != null)
                .collect(groupingBy(Contract::getPropertyId));

        List<PropertyFinancialSummary> properties = allPropertyIds.stream()
                .map(propId -> {
                    Property prop = propertiesById.get(propId);
                    if (prop == null) return null;
                    BigDecimal income = incomeByProperty.getOrDefault(propId, BigDecimal.ZERO);
                    BigDecimal expense = expensesByProperty.getOrDefault(propId, BigDecimal.ZERO);
                    BigDecimal netProfit = income.subtract(expense);
                    return new PropertyFinancialSummary(
                            propertyMapper.toSummary(prop),
                            income,
                            expense,
                            netProfit,
                            calculateOccupancyDays(contractsByProperty.getOrDefault(propId, List.of()), startDate, endDate)
                    );
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
                "EUR"
        );
    }

    // Helper methods

    private UUID getPropertyIdFromContract(UUID contractId, Map<UUID, Contract> contractsById) {
        Contract contract = contractsById.get(contractId);
        return contract != null ? contract.getPropertyId() : null;
    }

    private List<CategoryExpenseSummary> calculateExpensesByCategory(
            List<Expense> expenses,
            BigDecimal total) {

        Map<Expense.ExpenseCategory, BigDecimal> expensesByCategory = expenses.stream()
                .collect(groupingBy(
                        Expense::getCategory,
                        reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        Map<Expense.ExpenseCategory, Long> countsByCategory = expenses.stream()
                .collect(groupingBy(Expense::getCategory, counting()));

        return expensesByCategory.entrySet().stream()
                .map(entry -> {
                    double percentage = total.compareTo(BigDecimal.ZERO) > 0
                            ? entry.getValue().divide(total, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .doubleValue()
                            : 0.0;

                    return new CategoryExpenseSummary(
                            entry.getKey().name(),
                            entry.getValue(),
                            countsByCategory.get(entry.getKey()).intValue(),
                            Math.round(percentage * 100.0) / 100.0
                    );
                })
                .sorted(Comparator.comparing(CategoryExpenseSummary::total).reversed())
                .toList();
    }

    private int calculateOccupancyDays(List<Contract> contracts, LocalDate startDate, LocalDate endDate) {
        long totalDays = 0;
        for (Contract contract : contracts) {
            if (contract.getStatus() != ACTIVE) {
                continue;
            }

            LocalDate contractStart = contract.getStartDate();
            LocalDate contractEnd = contract.getEndDate() != null ? contract.getEndDate() : LocalDate.MAX;

            // Calculate overlap
            LocalDate overlapStart = contractStart.isBefore(startDate) ? startDate : contractStart;
            LocalDate overlapEnd = contractEnd.isAfter(endDate) ? endDate : contractEnd;

            if (!overlapStart.isAfter(overlapEnd)) {
                totalDays += ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1;
            }
        }

        return (int) totalDays;
    }
}
