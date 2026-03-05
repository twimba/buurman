package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record CaContractMetadata(
    @Nullable String province,
    @Nullable Boolean rentControlled,
    @Nullable MoneyAmount securityDepositAmount,
    @Nullable Integer securityDepositMonths,
    @Nullable Boolean tenancyBoardRegistered,
    @Nullable String energyRating)
    implements ContractCountryMetadata {}
