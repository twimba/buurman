package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record FrContractMetadata(
    @Nullable BigDecimal referenceRentPrice,
    @Nullable BigDecimal maxRentPrice,
    @Nullable Boolean zoneTendue,
    @Nullable Boolean loiAlurCompliant,
    @Nullable String diagnosticDpe,
    @Nullable Boolean leadPaintDiagnostic,
    @Nullable Boolean asbestosDiagnostic,
    @Nullable Boolean gasDiagnostic,
    @Nullable Boolean electricityDiagnostic,
    @Nullable Boolean erpDiagnostic,
    @Nullable Boolean furnishedLease,
    @Nullable BigDecimal cautionAmount,
    @Nullable Integer cautionMonths)
    implements ContractCountryMetadata {}
