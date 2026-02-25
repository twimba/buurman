package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record EsContractMetadata(
    @Nullable Boolean viviendaHabitual,
    @Nullable Boolean zonaTensionada,
    @Nullable BigDecimal referencePriceIndex,
    @Nullable BigDecimal fianzaAmount,
    @Nullable Integer fianzaMonths,
    @Nullable String energyCertificateRating,
    @Nullable BigDecimal garantiaAdicionalAmount,
    @Nullable Integer garantiaAdicionalMonths)
    implements ContractCountryMetadata {}
