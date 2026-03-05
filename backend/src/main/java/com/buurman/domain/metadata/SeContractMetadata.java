package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record SeContractMetadata(
    @Nullable String hyrestyp,
    @Nullable Boolean bruksvardessystemApplicable,
    @Nullable String energyDeclarationRating,
    @Nullable MoneyAmount depositAmount,
    @Nullable Integer depositMonths,
    @Nullable Boolean hyresnamndenEligible)
    implements ContractCountryMetadata {}
