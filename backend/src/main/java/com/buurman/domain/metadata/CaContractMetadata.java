package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record CaContractMetadata(
    @Nullable String province,
    @Nullable Boolean rentControlled,
    @Nullable BigDecimal securityDepositAmount,
    @Nullable Integer securityDepositMonths,
    @Nullable Boolean tenancyBoardRegistered,
    @Nullable String energyRating)
    implements ContractCountryMetadata {}
