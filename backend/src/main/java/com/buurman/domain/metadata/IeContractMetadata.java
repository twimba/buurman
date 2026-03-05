package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record IeContractMetadata(
    @Nullable String tenancyType,
    @Nullable String berRating,
    @Nullable MoneyAmount depositAmount,
    @Nullable Integer depositMonths,
    @Nullable Boolean rtbRegistered,
    @Nullable Boolean rentPressureZone,
    @Nullable MoneyAmount marketRentAmount,
    @Nullable String berCertificateNumber)
    implements ContractCountryMetadata {}
