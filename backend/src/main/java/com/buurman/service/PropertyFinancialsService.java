package com.buurman.service;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buurman.domain.identifier.PropertyIdentifier;
import com.buurman.domain.PropertyFee;
import com.buurman.domain.PropertyFinancing;
import com.buurman.domain.PropertyInsurance;
import com.buurman.domain.PropertyTax;
import com.buurman.domain.PropertyValuation;
import com.buurman.dto.response.PropertyFinancialSummaryResponse;
import com.buurman.repository.PropertyAcquisitionRepository;
import com.buurman.repository.PropertyFeeRepository;
import com.buurman.repository.PropertyFinancingRepository;
import com.buurman.repository.PropertyInsuranceRepository;
import com.buurman.repository.PropertyRepository;
import com.buurman.repository.PropertyTaxRepository;
import com.buurman.repository.PropertyValuationRepository;
import com.buurman.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PropertyFinancialsService {

  private static final int SCALE = 2;

  private final PropertyRepository propertyRepository;
  private final PropertyAcquisitionService acquisitionService;
  private final PropertyValuationService valuationService;
  private final PropertyFinancingService financingService;
  private final PropertyInsuranceService insuranceService;
  private final PropertyTaxService taxService;
  private final PropertyFeeService feeService;

  // Repositories for internal aggregate queries
  private final PropertyFinancingRepository financingRepository;
  private final PropertyValuationRepository valuationRepository;
  private final PropertyAcquisitionRepository acquisitionRepository;
  private final PropertyInsuranceRepository insuranceRepository;
  private final PropertyTaxRepository taxRepository;
  private final PropertyFeeRepository feeRepository;

  @Transactional(readOnly = true)
  @PreAuthorize("hasAnyRole('TEAM_ADMIN', 'TEAM_EDITOR', 'TEAM_VIEWER')")
  public PropertyFinancialSummaryResponse getFinancialSummary(
      PropertyIdentifier propertyIdentifier, UserPrincipal principal) {

    var acquisition = acquisitionService.getByProperty(propertyIdentifier, principal);
    var latestValuation = valuationService.getLatestByProperty(propertyIdentifier, principal);
    var valuationHistory = valuationService.listByProperty(propertyIdentifier, principal);
    var financings = financingService.listByProperty(propertyIdentifier, principal);
    var insurances = insuranceService.listByProperty(propertyIdentifier, principal);
    var taxes = taxService.listByProperty(propertyIdentifier, principal);
    var fees = feeService.listByProperty(propertyIdentifier, principal);

    UUID teamId = principal.requireTeamId();
    var property = propertyRepository.getByIdentifierAndTeamId(propertyIdentifier, teamId);
    UUID propertyId = property.getId();

    // Compute aggregates from active records
    Optional<BigDecimal> totalFinancingBalance = sumActiveFinancingBalances(propertyId, teamId);
    Optional<BigDecimal> totalAnnualInsurance = sumActiveAnnualInsurance(propertyId, teamId);
    Optional<BigDecimal> totalAnnualTaxes = sumActiveAnnualTaxes(propertyId, teamId);
    Optional<BigDecimal> totalAnnualFees = sumActiveAnnualFees(propertyId, teamId);

    Optional<BigDecimal> totalAnnualCosts =
        sumOptionals(totalAnnualInsurance, totalAnnualTaxes, totalAnnualFees);

    Optional<BigDecimal> latestValuationAmount = getLatestValuationAmount(propertyId, teamId);
    Optional<BigDecimal> netWorth =
        latestValuationAmount.map(val -> val.subtract(totalFinancingBalance.orElse(ZERO)));

    // Determine currency from acquisition or latest valuation
    Optional<String> currency =
        acquisition
            .flatMap(a -> a.purchasePriceCurrency())
            .or(() -> latestValuation.map(v -> v.currency()));

    return new PropertyFinancialSummaryResponse(
        acquisition,
        latestValuation,
        valuationHistory,
        financings,
        insurances,
        taxes,
        fees,
        totalFinancingBalance,
        totalAnnualInsurance,
        totalAnnualTaxes,
        totalAnnualFees,
        totalAnnualCosts,
        netWorth,
        currency);
  }

  // ===== Internal aggregate helpers (used by PropertyDashboardService) =====

  public Optional<BigDecimal> sumActiveFinancingBalances(UUID propertyId, UUID teamId) {
    List<PropertyFinancing> active =
        financingRepository.findActiveByPropertyIdAndTeamId(propertyId, teamId);
    if (active.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        active.stream()
            .map(f -> f.getCurrentBalance().orElse(f.getOriginalAmount()))
            .reduce(ZERO, BigDecimal::add));
  }

  public Optional<BigDecimal> getTotalMonthlyFinancingPayment(UUID propertyId, UUID teamId) {
    List<PropertyFinancing> active =
        financingRepository.findActiveByPropertyIdAndTeamId(propertyId, teamId);
    if (active.isEmpty()) {
      return Optional.empty();
    }
    BigDecimal total =
        active.stream()
            .filter(f -> !f.isPaymentVariable())
            .map(f -> f.getMonthlyPayment().orElse(ZERO))
            .reduce(ZERO, BigDecimal::add);
    return total.compareTo(ZERO) > 0 ? Optional.of(total) : Optional.empty();
  }

  public boolean hasVariablePaymentFinancing(UUID propertyId, UUID teamId) {
    return financingRepository.findActiveByPropertyIdAndTeamId(propertyId, teamId).stream()
        .anyMatch(PropertyFinancing::isPaymentVariable);
  }

  public Optional<BigDecimal> sumActiveAnnualInsurance(UUID propertyId, UUID teamId) {
    List<PropertyInsurance> active =
        insuranceRepository.findActiveByPropertyIdAndTeamId(propertyId, teamId);
    if (active.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        active.stream().map(PropertyInsurance::getAnnualPremium).reduce(ZERO, BigDecimal::add));
  }

  public Optional<BigDecimal> sumActiveAnnualTaxes(UUID propertyId, UUID teamId) {
    List<PropertyTax> active = taxRepository.findActiveByPropertyIdAndTeamId(propertyId, teamId);
    if (active.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        active.stream().map(PropertyTax::getAnnualAmount).reduce(ZERO, BigDecimal::add));
  }

  public Optional<BigDecimal> sumActiveAnnualFees(UUID propertyId, UUID teamId) {
    List<PropertyFee> active = feeRepository.findActiveByPropertyIdAndTeamId(propertyId, teamId);
    if (active.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        active.stream().map(PropertyFee::getAnnualAmount).reduce(ZERO, BigDecimal::add));
  }

  public Optional<BigDecimal> getLatestValuationAmount(UUID propertyId, UUID teamId) {
    return valuationRepository
        .findLatestByPropertyIdAndTeamId(propertyId, teamId)
        .map(PropertyValuation::getAmount);
  }

  /**
   * Computes per-month cost shares from active taxes, insurances, and fees. Returns a map of month
   * number (1-12) to total cost amount for that month.
   */
  public java.util.Map<Integer, BigDecimal> getMonthlyOperatingCosts(UUID propertyId, UUID teamId) {
    java.util.Map<Integer, BigDecimal> monthMap = new java.util.HashMap<>();

    // Taxes
    for (PropertyTax tax : taxRepository.findActiveByPropertyIdAndTeamId(propertyId, teamId)) {
      distributeAnnualCost(monthMap, tax.getAnnualAmount(), tax.getDueMonths().orElse(null));
    }

    // Insurances
    for (PropertyInsurance ins :
        insuranceRepository.findActiveByPropertyIdAndTeamId(propertyId, teamId)) {
      distributeAnnualCost(monthMap, ins.getAnnualPremium(), null);
    }

    // Fees
    for (PropertyFee fee : feeRepository.findActiveByPropertyIdAndTeamId(propertyId, teamId)) {
      distributeAnnualCost(monthMap, fee.getAnnualAmount(), fee.getDueMonths().orElse(null));
    }

    return monthMap;
  }

  private void distributeAnnualCost(
      java.util.Map<Integer, BigDecimal> monthMap,
      BigDecimal annualAmount,
      @org.jspecify.annotations.Nullable String dueMonths) {
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

  private static @org.jspecify.annotations.Nullable List<Integer> parseDueMonths(
      @org.jspecify.annotations.Nullable String dueMonths) {
    if (dueMonths == null || dueMonths.isBlank()) {
      return null;
    }
    return java.util.Arrays.stream(dueMonths.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(Integer::parseInt)
        .toList();
  }

  @SafeVarargs
  private static Optional<BigDecimal> sumOptionals(Optional<BigDecimal>... values) {
    BigDecimal sum = ZERO;
    boolean hasAny = false;
    for (Optional<BigDecimal> v : values) {
      if (v.isPresent()) {
        sum = sum.add(v.get());
        hasAny = true;
      }
    }
    return hasAny ? Optional.of(sum) : Optional.empty();
  }
}
