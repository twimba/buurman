package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record SeContractMetadata(
    @Nullable String hyrestyp,
    @Nullable Boolean bruksvardessystemApplicable,
    @Nullable String energyDeclarationRating,
    @Nullable BigDecimal depositAmount,
    @Nullable Integer depositMonths,
    @Nullable Boolean hyresnamndenEligible)
    implements ContractCountryMetadata {}
