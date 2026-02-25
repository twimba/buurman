package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record IeContractMetadata(
    @Nullable String tenancyType,
    @Nullable String berRating,
    @Nullable BigDecimal depositAmount,
    @Nullable Integer depositMonths,
    @Nullable Boolean rtbRegistered,
    @Nullable Boolean rentPressureZone,
    @Nullable BigDecimal marketRentAmount,
    @Nullable String berCertificateNumber)
    implements ContractCountryMetadata {}
