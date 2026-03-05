package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record EsContractMetadata(
    @Nullable Boolean viviendaHabitual,
    @Nullable Boolean zonaTensionada,
    @Nullable BigDecimal referencePriceIndex,
    @Nullable MoneyAmount fianzaAmount,
    @Nullable Integer fianzaMonths,
    @Nullable String energyCertificateRating,
    @Nullable MoneyAmount garantiaAdicionalAmount,
    @Nullable Integer garantiaAdicionalMonths)
    implements ContractCountryMetadata {}
