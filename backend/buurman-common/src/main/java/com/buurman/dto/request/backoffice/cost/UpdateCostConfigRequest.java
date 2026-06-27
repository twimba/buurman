package com.buurman.dto.request.backoffice.cost;

import com.buurman.util.SkipTestCoverage;

/** Update the editable provider cost parameters (EUR, major units). */
@SkipTestCoverage
public record UpdateCostConfigRequest(double mailgunBaseEur, double mailgunPerEmailEur) {}
