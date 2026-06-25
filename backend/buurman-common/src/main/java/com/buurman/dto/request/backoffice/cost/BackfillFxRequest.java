package com.buurman.dto.request.backoffice.cost;

import java.time.LocalDate;

/** Backfill daily FX rates from {@code since} through today. */
public record BackfillFxRequest(LocalDate since) {}
