package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record BeContractMetadata(
    @Nullable String region,
    @Nullable BigDecimal indexationBase,
    @Nullable String energyCertificateRating,
    @Nullable String energyCertificateNumber,
    @Nullable String registrationNumber,
    @Nullable Integer depositMonths,
    @Nullable String depositType,
    @Nullable String indexationBaseMonth)
    implements ContractCountryMetadata {}
