package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record FiContractMetadata(
    @Nullable String vuokrasopimustyyppi,
    @Nullable String energyCertificateRating,
    @Nullable BigDecimal vakuusAmount,
    @Nullable Integer vakuusMonths,
    @Nullable Boolean araRestricted)
    implements ContractCountryMetadata {}
