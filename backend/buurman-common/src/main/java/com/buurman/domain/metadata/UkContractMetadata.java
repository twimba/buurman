package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record UkContractMetadata(
    @Nullable String tenancyType,
    @Nullable String depositScheme,
    @Nullable String epcRating,
    @Nullable Boolean rightToRentChecked,
    @Nullable Boolean gasSafetyCertificate,
    @Nullable Boolean electricalSafetyCertificate,
    @Nullable MoneyAmount depositAmount)
    implements ContractCountryMetadata {}
