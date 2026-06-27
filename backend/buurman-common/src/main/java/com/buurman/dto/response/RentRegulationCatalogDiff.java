package com.buurman.dto.response;

import java.util.List;

import com.buurman.util.SkipTestCoverage;

/**
 * A preview of the changes a catalog reload would apply: the difference between the current
 * database reference data and the bundled catalog. Lets a backoffice admin review exactly what will
 * be added, removed and changed before performing the (destructive) reload.
 */
@SkipTestCoverage
public record RentRegulationCatalogDiff(
    String catalogVersion,
    String generatedAt,
    RentRegulationDiffCounts countries,
    RentRegulationDiffCounts regions,
    RentRegulationDiffCounts rules,
    List<RentRegulationCountryDiff> byCountry) {}
