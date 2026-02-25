package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record CyContractMetadata(
    @Nullable String tenancyType,
    @Nullable String epcRating,
    @Nullable BigDecimal depositAmount,
    @Nullable Integer depositMonths,
    @Nullable Boolean rentTribunalEligible,
    @Nullable BigDecimal commonExpensesAmount,
    @Nullable String municipalityRegistration)
    implements ContractCountryMetadata {}
