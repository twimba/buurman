package com.buurman.dto.request.backoffice.cost;

import com.buurman.util.SkipTestCoverage;

/** Set a provider's manual monthly cost, in EUR (major units, e.g. 19.99). */
@SkipTestCoverage
public record SetManualCostRequest(double amountEur) {}
