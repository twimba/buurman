package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

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
    @Nullable MoneyAmount totalServiceCostsAmount,
    @Nullable Boolean huurtoeslagEligible,
    @Nullable MoneyAmount liberalizationThreshold,
    @Nullable String energyLabel)
    implements ContractCountryMetadata {}
