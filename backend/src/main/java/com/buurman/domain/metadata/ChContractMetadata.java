package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record ChContractMetadata(
    @Nullable String canton,
    @Nullable String mietrechtRegion,
    @Nullable MoneyAmount nebenkostenAmount,
    @Nullable Integer kautionMonths,
    @Nullable MoneyAmount kautionAmount,
    @Nullable Boolean referenzzinssatzApplicable,
    @Nullable BigDecimal referenzzinssatz)
    implements ContractCountryMetadata {}
