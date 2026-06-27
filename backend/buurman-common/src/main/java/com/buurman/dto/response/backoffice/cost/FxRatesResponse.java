package com.buurman.dto.response.backoffice.cost;

import java.util.List;

/** Latest FX rate per currency, for the backoffice FX view. */
public record FxRatesResponse(List<FxRate> rates) {}
