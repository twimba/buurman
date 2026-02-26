package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record UkContractMetadata(
    @Nullable String tenancyType,
    @Nullable String depositScheme,
    @Nullable String epcRating,
    @Nullable Boolean rightToRentChecked,
    @Nullable Boolean gasSafetyCertificate,
    @Nullable Boolean electricalSafetyCertificate,
    @Nullable BigDecimal depositAmount)
    implements ContractCountryMetadata {}
