package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record MtContractMetadata(
    @Nullable String tenancyType,
    @Nullable String epcRating,
    @Nullable MoneyAmount depositAmount,
    @Nullable Integer depositMonths,
    @Nullable Boolean housingAuthorityRegistered,
    @Nullable MoneyAmount groundRent,
    @Nullable String rentalAgreementNumber)
    implements ContractCountryMetadata {}
