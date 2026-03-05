package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record FrContractMetadata(
    @Nullable MoneyAmount referenceRentPrice,
    @Nullable MoneyAmount maxRentPrice,
    @Nullable Boolean zoneTendue,
    @Nullable Boolean loiAlurCompliant,
    @Nullable String diagnosticDpe,
    @Nullable Boolean leadPaintDiagnostic,
    @Nullable Boolean asbestosDiagnostic,
    @Nullable Boolean gasDiagnostic,
    @Nullable Boolean electricityDiagnostic,
    @Nullable Boolean erpDiagnostic,
    @Nullable Boolean furnishedLease,
    @Nullable MoneyAmount cautionAmount,
    @Nullable Integer cautionMonths)
    implements ContractCountryMetadata {}
