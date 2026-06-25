package com.buurman.dto.request.backoffice.cost;

/** Start tracking a currency pair (source {@code currency} → EUR). */
public record AddFxPairRequest(String currency) {}
