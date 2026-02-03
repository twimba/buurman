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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

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

    public ReportService(PaymentRepository paymentRepository,
                        ExpenseRepository expenseRepository,
                        PropertyRepository propertyRepository,
                        ContractRepository contractRepository,
                        PropertyMapper propertyMapper) {
        this.paymentRepository = paymentRepository;
        this.expenseRepository = expenseRepository;
        this.propertyRepository = propertyRepository;
        this.contractRepository = contractRepository;
        this.propertyMapper = propertyMapper;
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

        // Get all payments in period (PAID status only)
        List<Payment> payments = paymentRepository.findByDateRange(startDate, endDate, teamId).stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.PAID)
                .filter(p -> propertyIds == null || propertyIds.isEmpty() ||
                        propertyIds.contains(getPropertyIdForPayment(p, teamId)))
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
                .collect(Collectors.groupingBy(
                        p -> getPropertyIdForPayment(p, teamId),
                        Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)
                ));

        // Calculate total expenses
        BigDecimal totalExpenses = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate expenses by category
        List<CategoryExpenseSummary> expensesByCategory = calculateExpensesByCategory(expenses, totalExpenses);

        // Calculate expenses by property
        Map<UUID, BigDecimal> expensesByProperty = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getPropertyId,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        // Combine property data
        Set<UUID> allPropertyIds = new HashSet<>();
        allPropertyIds.addAll(incomeByProperty.keySet());
        allPropertyIds.addAll(expensesByProperty.keySet());

        List<PropertyFinancialSummary> incomeByPropertyList = allPropertyIds.stream()
                .map(propId -> {
                    BigDecimal income = incomeByProperty.getOrDefault(propId, BigDecimal.ZERO);
                    return propertyRepository.findByIdAndTeamId(propId, teamId)
                            .map(prop -> new PropertyFinancialSummary(
                                    propertyMapper.toSummary(prop),
                                    income,
                                    BigDecimal.ZERO,
                                    income,
                                    calculateOccupancyDays(propId, startDate, endDate, teamId)
                            ))
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .toList();

        List<PropertyFinancialSummary> expensesByPropertyList = allPropertyIds.stream()
                .map(propId -> {
                    BigDecimal income = incomeByProperty.getOrDefault(propId, BigDecimal.ZERO);
                    BigDecimal expense = expensesByProperty.getOrDefault(propId, BigDecimal.ZERO);
                    BigDecimal netProfit = income.subtract(expense);

                    return propertyRepository.findByIdAndTeamId(propId, teamId)
                            .map(prop -> new PropertyFinancialSummary(
                                    propertyMapper.toSummary(prop),
                                    income,
                                    expense,
                                    netProfit,
                                    calculateOccupancyDays(propId, startDate, endDate, teamId)
                            ))
                            .orElse(null);
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
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months - 1).withDayOfMonth(1);

        List<IncomeTrendResponse.DataPoint> dataPoints = new ArrayList<>();

        for (int i = 0; i < months; i++) {
            YearMonth month = YearMonth.from(startDate.plusMonths(i));
            LocalDate monthStart = month.atDay(1);
            LocalDate monthEnd = month.atEndOfMonth();

            // Get paid payments for the month
            BigDecimal monthIncome = paymentRepository.findByDateRange(monthStart, monthEnd, teamId).stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.PAID)
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Get expenses for the month
            BigDecimal monthExpenses = expenseRepository.findByDateRange(monthStart, monthEnd, teamId).stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

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
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
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

        // Get all payments and expenses in period
        List<Payment> payments = paymentRepository.findByDateRange(startDate, endDate, teamId).stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.PAID)
                .toList();

        List<Expense> expenses = expenseRepository.findByDateRange(startDate, endDate, teamId);

        // Group by property
        Map<UUID, BigDecimal> incomeByProperty = payments.stream()
                .collect(Collectors.groupingBy(
                        p -> getPropertyIdForPayment(p, teamId),
                        Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)
                ));

        Map<UUID, BigDecimal> expensesByProperty = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getPropertyId,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        Set<UUID> allPropertyIds = new HashSet<>();
        allPropertyIds.addAll(incomeByProperty.keySet());
        allPropertyIds.addAll(expensesByProperty.keySet());

        List<PropertyComparisonResponse.PropertyData> propertyData = allPropertyIds.stream()
                .map(propId -> {
                    BigDecimal income = incomeByProperty.getOrDefault(propId, BigDecimal.ZERO);
                    BigDecimal expense = expensesByProperty.getOrDefault(propId, BigDecimal.ZERO);
                    BigDecimal netProfit = income.subtract(expense);

                    return propertyRepository.findByIdAndTeamId(propId, teamId)
                            .map(prop -> new PropertyComparisonResponse.PropertyData(
                                    propertyMapper.toSummary(prop),
                                    income,
                                    expense,
                                    netProfit
                            ))
                            .orElse(null);
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
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months - 1).withDayOfMonth(1);

        int totalProperties = (int) propertyRepository.findAllByTeamId(teamId).size();

        List<OccupancyTrendResponse.DataPoint> dataPoints = new ArrayList<>();

        for (int i = 0; i < months; i++) {
            YearMonth month = YearMonth.from(startDate.plusMonths(i));
            LocalDate monthStart = month.atDay(1);
            LocalDate monthEnd = month.atEndOfMonth();

            // Get active contracts for the month
            List<Contract> activeContracts = contractRepository.findAllByTeamId(teamId).stream()
                    .filter(c -> c.getStatus() == Contract.ContractStatus.ACTIVE)
                    .filter(c -> {
                        LocalDate contractStart = c.getStartDate();
                        LocalDate contractEnd = c.getEndDate() != null ? c.getEndDate() : LocalDate.MAX;
                        return !contractStart.isAfter(monthEnd) && !contractEnd.isBefore(monthStart);
                    })
                    .toList();

            int occupiedUnits = activeContracts.size();
            double occupancyRate = totalProperties > 0
                    ? (occupiedUnits * 100.0) / totalProperties
                    : 0.0;

            dataPoints.add(new OccupancyTrendResponse.DataPoint(
                    month.toString(),
                    Math.round(occupancyRate * 100.0) / 100.0,
                    totalProperties,
                    occupiedUnits
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

        // Get all paid payments for the year
        List<Payment> payments = paymentRepository.findByDateRange(startDate, endDate, teamId).stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.PAID)
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
                .collect(Collectors.groupingBy(
                        p -> getPropertyIdForPayment(p, teamId),
                        Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)
                ));

        Map<UUID, BigDecimal> expensesByProperty = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getPropertyId,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        Set<UUID> allPropertyIds = new HashSet<>();
        allPropertyIds.addAll(incomeByProperty.keySet());
        allPropertyIds.addAll(expensesByProperty.keySet());

        List<PropertyFinancialSummary> properties = allPropertyIds.stream()
                .map(propId -> {
                    BigDecimal income = incomeByProperty.getOrDefault(propId, BigDecimal.ZERO);
                    BigDecimal expense = expensesByProperty.getOrDefault(propId, BigDecimal.ZERO);
                    BigDecimal netProfit = income.subtract(expense);

                    return propertyRepository.findByIdAndTeamId(propId, teamId)
                            .map(prop -> new PropertyFinancialSummary(
                                    propertyMapper.toSummary(prop),
                                    income,
                                    expense,
                                    netProfit,
                                    calculateOccupancyDays(propId, startDate, endDate, teamId)
                            ))
                            .orElse(null);
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

    private UUID getPropertyIdForPayment(Payment payment, UUID teamId) {
        return contractRepository.findByIdAndTeamId(payment.getContractId(), teamId)
                .map(Contract::getPropertyId)
                .orElse(null);
    }

    private List<CategoryExpenseSummary> calculateExpensesByCategory(
            List<Expense> expenses,
            BigDecimal total) {

        Map<Expense.ExpenseCategory, BigDecimal> expensesByCategory = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        Map<Expense.ExpenseCategory, Long> countsByCategory = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getCategory, Collectors.counting()));

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

    private int calculateOccupancyDays(UUID propertyId, LocalDate startDate, LocalDate endDate, UUID teamId) {
        List<Contract> contracts = contractRepository.findByPropertyId(propertyId, teamId);

        long totalDays = 0;
        for (Contract contract : contracts) {
            if (contract.getStatus() != Contract.ContractStatus.ACTIVE) {
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
