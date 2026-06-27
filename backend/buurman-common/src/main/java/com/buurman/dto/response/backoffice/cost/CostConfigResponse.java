package com.buurman.dto.response.backoffice.cost;

import com.buurman.util.SkipTestCoverage;

/**
 * Admin-editable provider cost parameters. Mailgun has no money API, so its monthly figure is
 * estimated as {@code mailgunBaseEur} (flat plan fee) + accepted volume × {@code
 * mailgunPerEmailEur} (both EUR, major units).
 */
@SkipTestCoverage
public record CostConfigResponse(double mailgunBaseEur, double mailgunPerEmailEur) {}
