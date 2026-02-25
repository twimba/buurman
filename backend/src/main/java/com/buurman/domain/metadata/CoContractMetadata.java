package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record CoContractMetadata(
    @Nullable String tipoContrato,
    @Nullable BigDecimal depositoAmount,
    @Nullable Integer depositoMonths,
    @Nullable Boolean registraduriaInscribed,
    @Nullable BigDecimal administracionAmount,
    @Nullable String matriculaInmobiliaria,
    @Nullable Boolean estratoApplicable)
    implements ContractCountryMetadata {}
