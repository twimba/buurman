package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record NlContractMetadata(
    @Nullable String sectorClassification,
    @Nullable Integer wwsPoints,
    @Nullable Boolean huurcommissieEligible,
    @Nullable Boolean allInRent,
    @Nullable Boolean serviceGas,
    @Nullable Boolean serviceWater,
    @Nullable Boolean serviceElectricity,
    @Nullable Boolean serviceInternet,
    @Nullable Boolean serviceCleaning,
    @Nullable BigDecimal totalServiceCostsAmount,
    @Nullable Boolean huurtoeslagEligible,
    @Nullable BigDecimal liberalizationThreshold,
    @Nullable String energyLabel)
    implements ContractCountryMetadata {}
