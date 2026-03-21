package com.buurman.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.buurman.util.Generated;

@Generated
public record PropertyFinancialSummaryResponse(
    Optional<PropertyAcquisitionResponse> acquisition,
    Optional<PropertyValuationResponse> latestValuation,
    List<PropertyValuationResponse> valuationHistory,
    List<PropertyFinancingResponse> financings,
    List<PropertyInsuranceResponse> insurances,
    List<PropertyTaxResponse> taxes,
    List<PropertyFeeResponse> fees,
    Optional<BigDecimal> totalFinancingBalance,
    Optional<BigDecimal> totalAnnualInsurance,
    Optional<BigDecimal> totalAnnualTaxes,
    Optional<BigDecimal> totalAnnualFees,
    Optional<BigDecimal> totalAnnualCosts,
    Optional<BigDecimal> netWorth,
    Optional<String> currency) {}
