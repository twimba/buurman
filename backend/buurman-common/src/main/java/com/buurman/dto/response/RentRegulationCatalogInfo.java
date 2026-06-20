package com.buurman.dto.response;

import com.buurman.util.SkipTestCoverage;

/**
 * Metadata about the rent-regulation catalog bundled with the application, used to populate the
 * backoffice reload confirmation dialog before the (destructive) reload is performed.
 */
@SkipTestCoverage
public record RentRegulationCatalogInfo(
    String version,
    String generatedAt,
    String description,
    int countries,
    int regions,
    int rules) {}
