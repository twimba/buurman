package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record FiContractMetadata(
    @Nullable String vuokrasopimustyyppi,
    @Nullable String energyCertificateRating,
    @Nullable MoneyAmount vakuusAmount,
    @Nullable Integer vakuusMonths,
    @Nullable Boolean araRestricted)
    implements ContractCountryMetadata {}
