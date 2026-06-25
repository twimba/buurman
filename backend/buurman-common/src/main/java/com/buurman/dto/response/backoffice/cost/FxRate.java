package com.buurman.dto.response.backoffice.cost;

import java.time.LocalDate;

/** A stored FX rate: one unit of {@code currency} equals {@code rate} EUR on {@code rateDate}. */
public record FxRate(String currency, LocalDate rateDate, double rate, String source) {}
