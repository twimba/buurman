package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record CyContractMetadata(
    @Nullable String tenancyType,
    @Nullable String epcRating,
    @Nullable MoneyAmount depositAmount,
    @Nullable Integer depositMonths,
    @Nullable Boolean rentTribunalEligible,
    @Nullable MoneyAmount commonExpensesAmount,
    @Nullable String municipalityRegistration)
    implements ContractCountryMetadata {}
