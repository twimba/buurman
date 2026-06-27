package com.buurman.dto.response.backoffice.cost;

import com.buurman.util.SkipTestCoverage;

/**
 * One component of a provider's cost figure (e.g. Mailgun's "Plan" fee and its "12,480 emails"
 * volume line), so the page can show what drives the total. {@code amountMinor} is in the
 * provider's own currency (EUR for the providers that expose a breakdown today).
 */
@SkipTestCoverage
public record CostLineItem(String label, long amountMinor) {}
