package com.buurman.dto.response;

import com.buurman.util.SkipTestCoverage;

/**
 * Outcome of a backoffice rent-regulation catalog reload: the loaded catalog version and the number
 * of countries, regions and rules re-seeded from the bundled dataset.
 */
@SkipTestCoverage
public record RentRegulationReloadResult(
    String version,
    String generatedAt,
    int countriesLoaded,
    int regionsLoaded,
    int rulesLoaded) {}
