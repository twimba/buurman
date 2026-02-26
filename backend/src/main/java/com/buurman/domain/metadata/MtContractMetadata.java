package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record MtContractMetadata(
    @Nullable String tenancyType,
    @Nullable String epcRating,
    @Nullable BigDecimal depositAmount,
    @Nullable Integer depositMonths,
    @Nullable Boolean housingAuthorityRegistered,
    @Nullable BigDecimal groundRent,
    @Nullable String rentalAgreementNumber)
    implements ContractCountryMetadata {}
