package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record ChContractMetadata(
    @Nullable String canton,
    @Nullable String mietrechtRegion,
    @Nullable BigDecimal nebenkostenAmount,
    @Nullable Integer kautionMonths,
    @Nullable BigDecimal kautionAmount,
    @Nullable Boolean referenzzinssatzApplicable,
    @Nullable BigDecimal referenzzinssatz)
    implements ContractCountryMetadata {}
