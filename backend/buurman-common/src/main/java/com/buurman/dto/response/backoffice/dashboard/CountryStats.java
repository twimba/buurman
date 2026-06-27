package com.buurman.dto.response.backoffice.dashboard;

import com.buurman.util.SkipTestCoverage;

/**
 * Per-country rollup for the Geo map, grouped by the team's default country (ISO 3166-1 alpha-2).
 * {@code monthlyValueEurMinor} is the normalized monthly rent of active contracts (EUR cents).
 */
@SkipTestCoverage
public record CountryStats(
    String code, long teams, long properties, long contracts, long monthlyValueEurMinor) {}
