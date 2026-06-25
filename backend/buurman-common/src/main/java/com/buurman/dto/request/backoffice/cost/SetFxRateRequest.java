package com.buurman.dto.request.backoffice.cost;

import java.time.LocalDate;

/** Manually set the EUR rate for a currency. {@code date} is optional (defaults to today). */
public record SetFxRateRequest(double rate, LocalDate date) {}
