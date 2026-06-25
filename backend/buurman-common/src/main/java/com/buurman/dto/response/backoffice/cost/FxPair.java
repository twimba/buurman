package com.buurman.dto.response.backoffice.cost;

import java.time.LocalDate;
import java.util.Optional;

/** A tracked currency pair (source currency → EUR) with its latest stored rate, if any. */
public record FxPair(
    String currency,
    Optional<Double> rate,
    Optional<LocalDate> rateDate,
    Optional<String> source) {}
